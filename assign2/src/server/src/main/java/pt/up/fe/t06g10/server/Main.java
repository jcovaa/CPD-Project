package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.MessageEntity;
import pt.up.fe.t06g10.server.entity.RoomEntity;
import pt.up.fe.t06g10.server.entity.RoomMemberEntity;
import pt.up.fe.t06g10.server.entity.UserEntity;
import pt.up.fe.t06g10.server.repository.UserRepository;
import pt.up.fe.t06g10.server.room.RoomManager;
import pt.up.fe.t06g10.server.room.SessionManager;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Main <port>");
            System.exit(1);
        }

        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[0]);
            System.exit(1);
        }

        EntityManagerFactoryProvider.initialize(UserEntity.class, RoomEntity.class, RoomMemberEntity.class, MessageEntity.class);
        Runtime.getRuntime().addShutdownHook(new Thread(EntityManagerFactoryProvider::close));

        TokenService tokenService = new TokenService();
        UserRepository userRepository = new UserRepository();
        AuthService authService = new AuthService(userRepository, tokenService);
        SessionManager sessionManager = new SessionManager();
        RoomManager roomManager = new RoomManager();

        ChatServer server = new ChatServer(port, authService, tokenService, sessionManager, roomManager);
        server.start();
    }
}
