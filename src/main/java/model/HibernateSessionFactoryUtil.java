package model;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/*
Начнем с утилитного класса model.HibernateSessionFactoryUtil. У него всего одна задача — создавать для нашего приложения
фабрику сессий для работы с БД (привет, паттерн "Фабрика!").
Больше он ничего не умеет.
В этом классе мы создаем новый объект конфигураций Configuration, и передаем ему те классы, которые он должен воспринимать как сущности — model.Master и model.Client и т.д.
 */
public class HibernateSessionFactoryUtil {
    private static SessionFactory sessionFactory;

    private HibernateSessionFactoryUtil() {
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
                configuration.addAnnotatedClass(Master.class);
                configuration.addAnnotatedClass(Client.class);
                //configuration.addAnnotatedClass(MasterClients.class);
                configuration.addAnnotatedClass(Schedule.class);
                configuration.addAnnotatedClass(Visit.class);
                StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(builder.build());

            } catch (Exception e) {
                System.out.println("Исключение!" + e);
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }
}
