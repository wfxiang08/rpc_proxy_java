namespace java me.chunyu.rpc_proxy.service
exception RpcException {
  1: i32  code,
  2: string msg
}

service RpcServiceBase {
    void ping();
}