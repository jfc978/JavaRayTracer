import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.SwingUtilities;

import components.simplewriter.SimpleWriter;
import components.simplewriter.SimpleWriter1L;

public final class RayTracer {
    private RayTracer() {
    }

    // Global variables
    private static int WIDTH = RayMain.WIDTH;
    private static int HEIGHT = RayMain.HEIGHT;
    private static int SPP = RayMain.SPP;
    private static int MAX_DEPTH = RayMain.MAX_DEPTH;
    private static int THREADS = RayMain.THREADS;
    public static final double PI = 3.14159265;
    public static final int AMBIENT = 20;

    private static double rndDouble() {
        return ThreadLocalRandom.current().nextDouble();

    }

    //Convert to RGB
    //As this stage, the model is actually based in RGB, so this is a very rough way to manage RGB values
    //The ambient light term sets the minimum brightness of any ray returned to the camera, but is not necessary if tone mapping was implemented
    public static void toPixel(Vector color, final ImageWindow img,
            final int xCoord, final int yCoord) {
        color.x = AMBIENT + (color.x / SPP);
        if (color.x > 255) {
            color.x = 255;
        } else if (color.x < 0) {
            color.x = 0;
        }
        color.y = AMBIENT + (color.y / SPP);
        if (color.y > 255) {
            color.y = 255;
        } else if (color.y < 0) {
            color.y = 0;
        }
        color.z = AMBIENT + (color.z / SPP);
        if (color.z > 255) {
            color.z = 255;
        } else if (color.z < 0) {
            color.z = 0;
        }
        final Color clr = new Color((int) color.x, (int) color.y,
                (int) color.z);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                (Display.img).drawPixel(yCoord, xCoord, clr);
            }
        });

    }

    //Calculations converting from the pixelized camera plane to the virtual space.
    private static Vector camera(double x, double y) {
        double w = WIDTH;
        double h = HEIGHT;
        double viewX = PI / 4;
        double viewY = (h / w) * viewX;
        return new Vector(((2 * x - w) / w) * Math.tan(viewX),
                ((2 * y - h) / h) * Math.tan(viewY), -1.0);
    }

    //Random sampling method for diffuse interactions
    private static Vector hemisphere(double u1, double u2) {
        double r = rndDouble();
        double phi = 2 * PI * u2;
        return new Vector(Math.cos(phi) * r, Math.sin(phi) * r, u1);
    }

    //Recursive ray tracing function
    private static Vector trace(Ray ray, Environment rm, int depth, Vector clr,
            Halton h1, Halton h2) {
        //Check for max ray depth
        if (depth > RayMain.MAX_DEPTH) {
            return new Vector(0, 0, 0);
        }

        //Check for ray intersection
        Intersection intersect = rm.intersecting(ray);
        if (intersect.miss()) {
            return new Vector(0, 0, 0);
        }

        //Measure ray interaction
        Vector intPoint = (ray.origin)
                .vAdd(ray.direction.sMult(intersect.scalar));
        Vector normal = intersect.objHit.vNormal(intPoint);

        //Next ray starts at interaction point
        ray.origin = intPoint;

        //Color
        clr = clr.vAdd((new Vector(intersect.objHit.emittance,
                intersect.objHit.emittance, intersect.objHit.emittance))
                        .sMult(2));

        //Diffuse
        //Random reflection
        if (intersect.objHit.type == Material.DIFFUSE) {
            h1.next();
            h2.next();
            //Take a random ray from diffuse reflection
            //Each direction has equal probability
            ray.direction = (normal.vAdd(hemisphere(h1.get(), h2.get())));

            double cosine = (ray.direction).vdot(normal);
            Vector tmp = new Vector(0, 0, 0);

            //recursion on to next ray
            tmp = trace(ray, rm, depth + 1, tmp, h1, h2);

            //sum together light interactions
            clr.x += (cosine) * (tmp.x) * (intersect.objHit.color.x) * 0.1;
            clr.y += (cosine) * (tmp.y) * (intersect.objHit.color.y) * 0.1;
            clr.z += (cosine) * (tmp.z) * (intersect.objHit.color.z) * 0.1;

        }

        //Specular
        //Perfect reflection
        if (intersect.objHit.type == Material.SPECULAR) {
            double cosine = (ray.direction).vdot(normal);
            ray.direction = ((ray.direction).vSub(normal.sMult(2 * cosine)))
                    .norm();

            //Ray light contribution is cumulative
            Vector tmp = new Vector(0, 0, 0);
            tmp = trace(ray, rm, depth + 1, tmp, h1, h2);
            clr = clr.vAdd(tmp);
        }
        //Refractive
        if (intersect.objHit.type == Material.REFRACTIVE) {
            double n = intersect.objHit.refIndex;
            //if ray enters object
            if (normal.vdot(ray.direction) > 0) {
                normal = normal.sMult(-1.0);
                //negate next 1/n
                n = 1 / n;
            }
            n = 1 / n;
            double cosine1 = (normal.vdot(ray.direction)) * (-1.0);
            double cosine2 = 1.0 - n * n * (1.0 - cosine1 * cosine1);
            if (cosine2 > 0) {
                ray.direction = (ray.direction.sMult(n))
                        .vAdd(normal.sMult(n * cosine1 - Math.sqrt(cosine2)));
                ray.direction = ray.direction.norm();

                //Ray light contribution is cumulative
                Vector tmp = new Vector(0, 0, 0);
                tmp = trace(ray, rm, depth + 1, tmp, h1, h2);
                clr = clr.vAdd(tmp);
            } else {
                //total refraction, not like returned to ray
                return new Vector(0, 0, 0);
            }
        }
        return clr;
    }

    //Create environment and send out sampling rays
    public static Environment render(double refr_index) {
        Environment rm = new Environment();

        // Radius, position, color, emission, type (1=diff, 2=spec, 3=refr) for spheres
        Objects sphere1 = new Sphere(1.05, new Vector(1.45, -3.0, -4.4));
        sphere1.setProp(new Vector(12, 12, 0), 0.0, Material.DIFFUSE);
        rm.add(sphere1); // Middle sphere

        Objects sphere2 = new Sphere(1.0, new Vector(0.0, 0.0, -6.0));
        sphere2.setProp(new Vector(10, 10, 1), 5.0, Material.REFRACTIVE);
        sphere2.refIndex = 4.0; //Similar to glass
        rm.add(sphere2); // Right sphere

        Objects sphere3 = new Sphere(0.6, new Vector(1.95, -1.75, -3.1));
        sphere3.setProp(new Vector(4, 4, 12), 0.0, Material.DIFFUSE);
        rm.add(sphere3); // Left sphere

        Objects sphere4 = new Sphere(0.3, new Vector(0.0, 3.0, -8.0));
        sphere4.setProp(new Vector(12, 12, 12), 100.0, Material.DIFFUSE);
        rm.add(sphere4); // Light sphere

        Objects sphere5 = new Sphere(0.6, new Vector(1.95, -1.75, -8.0));
        sphere5.setProp(new Vector(12, 12, 12), 0.0, Material.SPECULAR);
        rm.add(sphere5); // Rear sphere

        // Position, normal, color, emission, type for planes
        Objects plane1 = new Plane(2.5, new Vector(-1, 0, 0));
        plane1.setProp(new Vector(6, 6, 6), 0.0, Material.DIFFUSE);
        rm.add(plane1); // Bottom plane

        Objects plane2 = new Plane(5.0, new Vector(0, 0, 1));
        plane2.setProp(new Vector(6, 6, 6), 0.0, Material.REFRACTIVE);
        plane2.refIndex = 2.0;
        rm.add(plane2); // Back window

        Objects plane3 = new Plane(5, new Vector(0, 1, 0));
        plane3.setProp(new Vector(8, 8, 8), 60.0, Material.DIFFUSE);
        rm.add(plane3); // Left plane

        Objects plane4 = new Plane(2.75, new Vector(-1, -1, 0));
        plane4.setProp(new Vector(10, 3, 10), 0.0, Material.DIFFUSE);
        rm.add(plane4); // Right plane

        Objects plane5 = new Plane(3.0, new Vector(1, 0, 0));
        plane5.setProp(new Vector(6, 6, 6), 0.0, Material.DIFFUSE);
        rm.add(plane5); // Ceiling plane

        Objects plane6 = new Plane(0.5, new Vector(0, 0, -1));
        plane6.setProp(new Vector(6, 6, 6), 0.0, Material.SPECULAR);
        rm.add(plane6); // Front plane

        Objects plane7 = new Plane(12.0, new Vector(0, 0, 1));
        plane7.setProp(new Vector(8, 8, 8), 0.0, Material.DIFFUSE);
        rm.add(plane7); // Back plane

        return rm;
    }

    public static void raySampling(int x1, int x2, int height, int samples,
            ImageWindow img, Environment rm) {
        SimpleWriter out = new SimpleWriter1L();
        //Initialize Halton Sequences
        Halton h1, h2;
        h1 = new Halton(0, 2);
        h2 = new Halton(0, 2);

        //Run ray samples for every point
        Vector[][] pix = new Vector[WIDTH][HEIGHT];
        if (x2 > WIDTH) {
            x2 = WIDTH;
        }
        for (int i = x1; i < x2; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                out.println("Generating pix [" + i + "," + j + "]");
                pix[i][j] = new Vector(0, 0, 0);
                for (int s = 0; s < samples; s++) {
                    //initial color set to zero
                    Vector color = new Vector(0, 0, 0);
                    //generate camera ray to pizel(i,j)
                    Vector cam = camera(i, j);
                    //Monte carlo pixel sampling method
                    cam.x = cam.x + rndDouble() / 1000;
                    cam.y = cam.y + rndDouble() / 1000;

                    //Define initial camera ray
                    Ray ray = new Ray(new Vector(0, 0, 0),
                            (cam.vSub(new Vector(0, 0, 0))).norm());

                    //Begin tracing
                    color = trace(ray, rm, 0, color, h1, h2);

                    //Add color samples
                    pix[i][j].x += color.x;
                    pix[i][j].y += color.y;
                    pix[i][j].z += color.z;
                }
                //Draw picture pixel by pixel
                Vector vec = pix[i][j];
                toPixel(vec, img, i, j);
            }
        }
    }

    //Class for implementing multithreading
    public static class raySplit implements Runnable {
        int x1, x2, height;
        int samplesNum;
        ImageWindow imag;
        Environment enr;

        public raySplit(int begin, int width, int height, int samples,
                ImageWindow img, Environment rm) {
            this.x1 = begin;
            this.x2 = begin + width;
            this.samplesNum = samples;
            this.imag = img;
            this.enr = rm;
            this.height = height;
        }

        @Override
        public void run() {
            raySampling(this.x1, this.x2, this.height, this.samplesNum,
                    this.imag, this.enr);
        }
    }

    //Get starting values from main class
    public static void update() {
        WIDTH = RayMain.WIDTH;
        HEIGHT = RayMain.HEIGHT;
        SPP = RayMain.SPP;
        MAX_DEPTH = RayMain.MAX_DEPTH;
        THREADS = RayMain.THREADS;
    }

    public static void main(ImageWindow img) throws InterruptedException {
        SimpleWriter out = new SimpleWriter1L();

        //Load variables
        update();

        //Create Objects Environment
        Environment rm = render(1.6);

        //Start Clock
        Instant t1 = Instant.now();

        //Split Ray Tracing across Multiple Threads
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(new raySplit(i * HEIGHT / THREADS,
                    (HEIGHT / THREADS) + THREADS, WIDTH, SPP, img, rm));
        }
        for (int i = 0; i < THREADS; i++) {
            threads[i].start();
        }
        for (int i = 0; i < THREADS; i++) {
            threads[i].join();
        }

        //Stop Clock
        Instant t2 = Instant.now();
        Duration dr = Duration.between(t1, t2);

        long nanos = dr.toNanos();
        double nano = nanos;
        nano = nano / (Math.pow(10, 9));

        out.println("Time Elapsed " + nano);
        out.println("Success");
        out.close();
    }
}
