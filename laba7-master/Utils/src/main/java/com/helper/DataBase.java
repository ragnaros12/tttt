package com.helper;

import com.helper.objects.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class DataBase {
    private static Connection sqlServer;
    private static DataBase Instance;
    private static MessageDigest Sha1;

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static String createScript = "CREATE table IF NOT EXISTS Users(" +
            "id serial PRIMARY KEY," +
            "login varchar(255)," +
            "password varchar(255)" + ");" +
            "CREATE table IF NOT EXISTS Car" + "(" +
            " cool boolean NOT NULL," +
            " car_id serial PRIMARY KEY" + ");" +
            "CREATE table IF NOT EXISTS Coordinates" + "(" +
            " x real NOT NULL," +
            " y integer NOT NULL," +
            " coor_id serial PRIMARY KEY" + ");" + "" + "" +
            "CREATE table IF NOT EXISTS HumanBeing" + "(" +
            " id serial PRIMARY KEY," +
            " name VARCHAR(255)," +
            " coordinate_id integer NOT NULL," +
            " car_id integer NOT NULL," +
            " creationdate date NOT NULL," +
            " realhero boolean NOT NULL," +
            " hastoothpick boolean NOT NULL," +
            " impactspeed integer NOT NULL," +
            " weapontype varchar(255) NOT NULL," +
            " mood varchar(255) NOT NULL," +
            " creator integer NOT NULL," +
            " FOREIGN KEY (car_id)" +
            " REFERENCES Car (car_id) MATCH SIMPLE" +
            " ON UPDATE CASCADE" +
            " ON DELETE CASCADE," +
            " FOREIGN KEY (coordinate_id)" +
            " REFERENCES Coordinates (coor_id) MATCH SIMPLE" +
            " ON UPDATE CASCADE" +
            " ON DELETE CASCADE" + ")";

    public static String[] args;

    public static void setArgs(String[] args) {
        DataBase.args = args;
    }

    public DataBase() throws Exception {
        Class.forName("org.postgresql.Driver");
        sqlServer = DriverManager.getConnection(args[0], args[1] , args[2]);
        Sha1 = MessageDigest.getInstance("Sha1");

        PreparedStatement statement = sqlServer.prepareStatement(createScript);
        statement.execute();
    }

    public synchronized static DataBase getInstance() {
        try {
            if (Instance == null) {
                Instance = new DataBase();
            }
            return Instance;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public synchronized TreeSet<HumanBeing> getAll() throws SQLException {
        PreparedStatement preparedStatement = sqlServer.prepareStatement(
                "SELECT * from HumanBeing " +
                        "INNER JOIN Coordinates ON Coordinates.coor_id = HumanBeing.coordinate_id " +
                        "INNER JOIN Car ON Car.car_id = HumanBeing.car_id "
        );

        ResultSet set = preparedStatement.executeQuery();
        TreeSet<HumanBeing> humanBeings = new TreeSet<>();
        while (set.next()) {
            HumanBeing humanBeing = new HumanBeing();
            humanBeing.setCar(new Car(set.getBoolean(set.findColumn("car_cool"))));
            humanBeing.setCoordinates(new Coordinates(
                    set.getDouble(set.findColumn("coordinates_x")),
                    set.getInt(set.findColumn("coordinates_y"))
            ));

            humanBeing.setIdCreator(set.getInt(set.findColumn("IdCreator")));
            humanBeing.setName(set.getString(set.findColumn("name")));
            humanBeing.setCreationDate(set.getDate(set.findColumn("creationdate")));
            humanBeing.setHasToothpick(set.getBoolean(set.findColumn("hastoothpick")));
            humanBeing.setId(set.getLong(set.findColumn("id")));
            humanBeing.setImpactSpeed(set.getInt(set.findColumn("impactspeed")));
            humanBeing.setMood(Mood.valueOf(set.getString(set.findColumn("mood")).toUpperCase(Locale.ROOT)));
            humanBeing.setRealHero(set.getBoolean(set.findColumn("realhero")));
            humanBeing.setWeaponType(WeaponType.valueOf(set.getString(set.findColumn("weapontype")).toUpperCase(Locale.ROOT)));
            humanBeings.add(humanBeing);
        }
        return humanBeings;

    }

    public synchronized void Clear() throws SQLException {
        PreparedStatement preparedStatement = sqlServer.prepareStatement(
                "DELETE from HumanBeing; DELETE from Coordinates; DELETE from Car"
        );
        preparedStatement.execute();
    }

    public synchronized void Add(HumanBeing humanBeing) throws SQLException {
        int carId = AddCar(humanBeing.getCar());
        int coorId = AddCoordinates(humanBeing.getCoordinates());

        PreparedStatement preparedStatement = sqlServer.prepareStatement(
                "INSERT INTO HumanBeing(" +
                        "name, coordinate_id, carid, creationdate, realhero, hastoothpick, impactspeed, weapontype, mood, IdCreator)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
        );
        preparedStatement.setString(1,humanBeing.getName());
        preparedStatement.setInt(2,coorId);
        preparedStatement.setInt(3,carId);
        preparedStatement.setDate(4, new Date(new java.util.Date().getTime()));
        preparedStatement.setBoolean(5, humanBeing.getRealHero());
        preparedStatement.setBoolean(6, humanBeing.isHasToothpick());
        preparedStatement.setInt(7, humanBeing.getImpactSpeed());
        preparedStatement.setString(8, humanBeing.getWeaponType().toString().toLowerCase());
        preparedStatement.setString(9, humanBeing.getMood().toString().toLowerCase());
        preparedStatement.setInt(10, humanBeing.getIdCreator());

        preparedStatement.execute();
    }
    public synchronized int AddCoordinates(Coordinates c) throws SQLException {
        PreparedStatement preparedStatement = sqlServer.prepareStatement("INSERT INTO Coordinates" +
                "(coordinates_x, coordinates_y)" +
                "VALUES (?, ?) RETURNING coordinates_id");
        preparedStatement.setDouble(1, c.getX());
        preparedStatement.setInt(2, c.getY());
        ResultSet s = preparedStatement.executeQuery();
        s.next();
        return s.getInt(1);
    }
    public synchronized int AddCar(Car c) throws SQLException {
        PreparedStatement preparedStatement = sqlServer.prepareStatement("INSERT INTO Car" +
                "(car_cool)" +
                "VALUES (?) RETURNING car_id");
        preparedStatement.setBoolean(1, c.getCool());
        ResultSet s = preparedStatement.executeQuery();
        s.next();
        return s.getInt(1);
    }


    public synchronized void AddUser(String login, String password) throws SQLException {
        PreparedStatement preparedStatement = sqlServer.prepareStatement(
                "INSERT INTO Users(" +
                        "login, password)" +
                        "VALUES (?, ?)"
        );
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, new BigInteger(1, Sha1.digest(password.getBytes())).toString(16));
        preparedStatement.execute();
    }

    public synchronized Integer Login(String login, String password){
        try {
            PreparedStatement preparedStatement = sqlServer.prepareStatement(
                    "SELECT * FROM Users where login = ? AND password = ?"
            );
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, new BigInteger(1, Sha1.digest(password.getBytes())).toString(16));
            ResultSet set = preparedStatement.executeQuery();
            if (!set.next())
                return null;
            return set.getInt(set.findColumn("Id"));
        }
        catch (Exception e){
            return null;
        }
    }

    public void Remove(Long id)throws Exception{
        PreparedStatement preparedStatement = sqlServer.prepareStatement(
                "DELETE from HumanBeing where id = ?"
        );
        preparedStatement.setLong(1, id);
        preparedStatement.execute();
    }


    public void Update(HumanBeing h) throws Exception{
        Remove(h.getId());
        Add(h);
    }

}
