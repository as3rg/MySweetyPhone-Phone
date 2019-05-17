package Utils;

public class SimpleIntegerProperty {
    int value;

    SimpleIntegerProperty(int value){
        this.value = value;
    }

    public void set(int value){
        this.value = value;
    }

    public int get(){
        return value;
    }
}
