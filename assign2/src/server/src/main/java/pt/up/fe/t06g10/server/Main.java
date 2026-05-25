package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.MessageEntity;
import pt.up.fe.t06g10.server.entity.RoomEntity;
import pt.up.fe.t06g10.server.entity.RoomMemberEntity;
import pt.up.fe.t06g10.server.entity.UserEntity;
import pt.up.fe.t06g10.server.repository.MessageRepository;
import pt.up.fe.t06g10.server.repository.RoomMemberRepository;
import pt.up.fe.t06g10.server.repository.RoomRepository;
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

        ChatServer server = getServer(port);
        server.start();
    }

    private static ChatServer getServer(int port) {
        TokenService tokenService = new TokenService();
        UserRepository userRepository = new UserRepository();
        AuthService authService = new AuthService(userRepository, tokenService);
        RoomRepository roomRepository = new RoomRepository();
        RoomMemberRepository roomMemberRepository = new RoomMemberRepository();
        MessageRepository messageRepository = new MessageRepository();
        SessionManager sessionManager = new SessionManager();
        RoomManager roomManager = new RoomManager(roomRepository, roomMemberRepository, messageRepository, userRepository);

        return new ChatServer(port, authService, tokenService, sessionManager, roomManager);
    }
}
