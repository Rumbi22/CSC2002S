package MonteCarloMini;

import java.util.Random;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

// Class for parallel Monte Carlo minimization using ForkJoinPool
class MonteCarloMinimizationParallel extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 100; // Adjust as needed
    private int start, end;
    private Search[] searches;
    private TerrainArea terrain;

    static final boolean DEBUG=false;

    static long startTime = 0;
    static long endTime = 0;

    //timers - note milliseconds

    private static void tick(){
        startTime = System.currentTimeMillis();
    }
    private static void tock(){
        endTime=System.currentTimeMillis();
    }


    public MonteCarloMinimizationParallel(int start, int end, Search[] searches, TerrainArea terrain) {
        this.start = start;
        this.end = end;
        this.searches = searches;
        this.terrain = terrain;
    }

    @Override
    protected Integer compute() {
        if (end - start <= THRESHOLD) {
            // Perform searches sequentially within the range
            int min = Integer.MAX_VALUE;
            for (int i = start; i < end; i++) {
                int localMin = searches[i].find_valleys();
                if (!searches[i].isStopped() && localMin < min) {
                    min = localMin;
                }
            }
            return min;
        } else {
            // Divide the range into smaller subtasks
            int mid = start + (end - start) / 2;
            MonteCarloMinimizationParallel leftTask = new MonteCarloMinimizationParallel(start, mid, searches, terrain);
            MonteCarloMinimizationParallel rightTask = new MonteCarloMinimizationParallel(mid, end, searches, terrain);

            leftTask.fork();
            int rightResult = rightTask.compute();
            int leftResult = leftTask.join();

            // Combine results
            return Math.min(leftResult, rightResult);
        }
    }

    //public class ParallelMonteCarloMain {
    public static void main(String[] args) {
        // Initialize and populate rows, columns, etc.

        int rows, columns; //grid size
        double xmin, xmax, ymin, ymax; //x and y terrain limits
        TerrainArea terrain;  //object to store the heights and grid points visited by searches
        double searches_density;    // Density - number of Monte Carlo  searches per grid position - usually less than 1!

        int num_searches;        // Number of searches
        Search[] searches;        // Array of searches
        Random rand = new Random();  //the random number generator

        if (args.length != 7) {
            System.out.println("Incorrect number of command line arguments provided.");
            System.exit(0);
        }
        /* Read argument values */
        rows = Integer.parseInt(args[0]);
        columns = Integer.parseInt(args[1]);
        xmin = Double.parseDouble(args[2]);
        xmax = Double.parseDouble(args[3]);
        ymin = Double.parseDouble(args[4]);
        ymax = Double.parseDouble(args[5]);
        searches_density = Double.parseDouble(args[6]);

        tick();


        terrain = new TerrainArea(rows, columns, xmin, xmax, ymin, ymax);
        num_searches = (int) (rows * columns * searches_density);
        searches = new Search[num_searches];
        // Populate searches array

        for (int i = 0; i < num_searches; i++) {
            searches[i] = new Search(i + 1, rand.nextInt(rows), rand.nextInt(columns), terrain);
        }

        // Create a ForkJoinPool and execute the parallel task
        ForkJoinPool pool = new ForkJoinPool();
        int globalMin = pool.invoke(new MonteCarloMinimizationParallel(0, num_searches, searches, terrain));

        tock();

        // Output globalMin and other results
        System.out.printf("Run parameters\n");
        System.out.printf("\t Rows: %d, Columns: %d\n", rows, columns);
        System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax );
        System.out.printf("\t Search density: %f (%d searches)\n", searches_density,num_searches );

        /*  Total computation time */
        System.out.printf("Time: %d ms\n",endTime - startTime );
        int tmp=terrain.getGrid_points_visited();
        System.out.printf("Grid points visited: %d  (%2.0f%s)\n",tmp,(tmp/(rows*columns*1.0))*100.0, "%");
        tmp=terrain.getGrid_points_evaluated();
        System.out.printf("Grid points evaluated: %d  (%2.0f%s)\n",tmp,(tmp/(rows*columns*1.0))*100.0, "%");


        System.out.println("Global minimum: " + globalMin);
    }
}



