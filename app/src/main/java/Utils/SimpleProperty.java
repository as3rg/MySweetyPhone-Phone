package Utils;

public class SimpleProperty<T> {
    private T value;

    public SimpleProperty(T value){
        this.value = value;
    }

    public void set(T value){
        this.value = value;
    }

    public T get(){
        return value;
    }
}
