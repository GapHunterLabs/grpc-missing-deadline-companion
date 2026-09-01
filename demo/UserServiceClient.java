import java.util.concurrent.TimeUnit;

class UserServiceClient {

    // Flagged: no deadline anywhere in the chain.
    GetUserResponse fetchUnsafe(io.grpc.Channel channel, GetUserRequest request) {
        return UserServiceGrpc.newBlockingStub(channel).getUser(request);
    }

    // Not flagged: withDeadlineAfter present before the RPC call.
    GetUserResponse fetchSafe(io.grpc.Channel channel, GetUserRequest request) {
        return UserServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getUser(request);
    }
}
