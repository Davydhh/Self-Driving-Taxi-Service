public class SmartCity {
    private static SmartCity instance = null;

    private static final int rows = 10;
    private static final int columns = 10;

    private static final Integer[][] smartCity = new Integer[rows][columns];

    private SmartCity() {
        for(int i = 0; i < rows; i++)
            for(int j = 0; j < columns; j++)
                smartCity[i][j] = 0;
    }

    public static SmartCity getInstance() {
        // Crea l'oggetto solo se NON esiste:
        if (instance == null) {
            instance = new SmartCity();
        }
        return instance;
    }

    public static synchronized void addTaxi(int x, int y) {
        smartCity[x][y]++;
    }
}
