

public class Main {
    private Main() {
    }

    public static Main getInstance() {
        return InnerMain.instance;
    }

    private static class InnerMain {
        private static Main instance = new Main();
    }

    public static void main(String[] args) {
        Main m = getInstance();
        System.out.println(m);
        Main m2 = InnerMain.instance;
        System.out.println(m2);
    }
}


