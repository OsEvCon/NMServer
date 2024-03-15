package model;

public class User {

    private Integer id;
    private String name;

    private String telegram_Id;

    private String chat_Id;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelegram_Id() {
        return telegram_Id;
    }

    public void setTelegram_Id(String telegram_Id) {
        this.telegram_Id = telegram_Id;
    }

    public String getChat_Id() {
        return chat_Id;
    }

    public void setChat_Id(String chat_Id) {
        this.chat_Id = chat_Id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
