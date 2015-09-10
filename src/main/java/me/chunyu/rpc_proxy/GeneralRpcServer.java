package me.chunyu.rpc_proxy;


import me.chunyu.rpc_proxy.server.TNonblockingServer;
import me.chunyu.rpc_proxy.zk.CuratorRegister;
import me.chunyu.rpc_proxy.zk.ServiceEndpoint;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;

public class GeneralRpcServer extends TNonblockingServer {
    protected String serviceName;
    protected String[] productNames;
    protected ConfigFile config;

    protected ServiceEndpoint[] endpoints;
    protected CuratorRegister curator;

    public GeneralRpcServer(TProcessor processor, String configPath) {
        super(processor);
        config = new ConfigFile(configPath);

        this.serviceName = config.service;
        this.productNames = config.productName.split(",");

        if (config.workers < 1) {
            throw new RuntimeException("Invalid Worker Number");
        }
        setUp(config.workers, 5000);

        // sort out exit handler

        String hostport = String.format("%s:%d", config.getFrontHost(), config.frontPort);
        String serviceId = ServiceEndpoint.getServiceId(hostport);
        endpoints = new ServiceEndpoint[this.productNames.length];
        int i =0;
        for (String productName: this.productNames) {
            ServiceEndpoint endpoint = new ServiceEndpoint(productName, serviceName, serviceId, hostport);
            endpoints[i] = endpoint;
            i ++;
        }
        curator = new CuratorRegister(config.zkAddr);
        curator.getCurator().start();


        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                GeneralRpcServer.this.stop();
            }
        }));
    }




    @Override
    public void stop() {
        // 下线服务(等待LB先将自己下线)
        setServing(false);


        // 然后等待结束服务:
        while (System.currentTimeMillis() - lastRequestTime.get() < 5000) {
            try {
                LOG.info("Sleeping 1 seconds");
                Thread.sleep(1000);
            } catch (Exception e) {
                //
            }
        }

        // 如何停止这些服务呢?
        super.stop();
    }

    @Override
    public void serve() {
        // 创建Server Socket
        TNonblockingServerSocket socket;

        try {
            InetSocketAddress address = new InetSocketAddress(config.getFrontHost(), config.frontPort);
            socket = new TNonblockingServerSocket(address);
        } catch (Exception e) {
            LOG.warn("Socket Error: ", e);
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
        // 设置Server Socket
        setServerTransport(socket);

        // 启动Server
        super.serve();
    }

    @Override
    protected void setServing(boolean serving) {
        if (this.isServing() == serving) {
            return;
        }

        super.setServing(serving);
        try {
            // 在服务启动或停止时注册服务
            // TODO: 如果zk的session过期，如何重新注册呢? 如果出现异常又改如何处理呢?
            if (serving) {
                for (ServiceEndpoint endpoint: endpoints) {
                    endpoint.addServiceEndpoint(curator);
                }
            } else {
                for (ServiceEndpoint endpoint: endpoints) {
                    endpoint.deleteServiceEndpoint(curator);
                }
            }
        } catch(Exception e) {
            LOG.warn("Service Register Error", e);
        }
    }
}
