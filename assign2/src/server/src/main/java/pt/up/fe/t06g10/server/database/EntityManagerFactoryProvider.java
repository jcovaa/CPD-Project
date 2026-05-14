package pt.up.fe.t06g10.server.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.HashMap;
import java.util.Map;

public final class EntityManagerFactoryProvider {
    private static SessionFactory sessionFactory;

    private EntityManagerFactoryProvider() {
    }

    public static synchronized void initialize(Class<?>... annotatedClasses) {
        if (sessionFactory != null) {
            return;
        }

        Map<String, Object> settings = getSettings();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings).build();

        MetadataSources sources = new MetadataSources(registry);
        for (Class<?> annotatedClass : annotatedClasses) {
            sources.addAnnotatedClass(annotatedClass);
        }

        sessionFactory = sources.buildMetadata().buildSessionFactory();
    }

    private static Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", DatabaseConfig.getJdbcUrl());
        settings.put("hibernate.connection.username", DatabaseConfig.getUsername());
        settings.put("hibernate.connection.password", DatabaseConfig.getPassword());
        settings.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "update");
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.format_sql", "false");
        return settings;
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not initialized");
        }
        return sessionFactory;
    }

    public static Session openSession() {
        return getSessionFactory().openSession();
    }

    public static synchronized void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
