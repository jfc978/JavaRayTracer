import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;

//Main Class for Ray Tracing Application
class RayMain {
    private RayMain() {
    }

    //Global variables
    public static int WIDTH = 300;
    public static int HEIGHT = 300;
    public static int SPP = 30;
    public static int MAX_DEPTH = 10;
    public static int THREADS = 8;

    public static void update() {
        SPP = Display.SPP;
        MAX_DEPTH = Display.MAX_DEPTH;
        THREADS = Display.THREADS;
    }

    public static void callRayMain(ImageWindow img) {
        try {
            RayTracer.main(img);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Display.main();
        callRayMain(Display.img);
    }
}

//Additional Classes Used Throughout the Application
//Graphics Structure
class ImageWindow extends JPanel {
    private static final long serialVersionUID = 1L;
    int width;
    int height;
    BufferedImage img;

    ImageWindow(int w, int h) {
        this.img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        this.width = w;
        this.height = h;
        this.setPreferredSize(new Dimension(w, h));
    }

    public void drawPixel(int x, int y, Color clr) {
        Graphics g = this.img.getGraphics();
        g.setColor(Color.WHITE); //Border
        g.drawRect(0, 0, this.width - 2, this.height - 2);

        g.setColor(clr); //Replace with clr;
        g.drawLine(x, y, x, y);

        g.dispose();
        this.repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(this.img, 0, 0, null);
    }
}

//Vectors in 3-D
//Basic arithmetic, with some assumptions
class Vector {
    double x, y, z;

    Vector(double x0, double y0, double z0) {
        this.x = x0;
        this.y = y0;
        this.z = z0;
    }

    Vector vAdd(Vector vec) {
        return new Vector(this.x + vec.x, this.y + vec.y, this.z + vec.z);
    }

    Vector vSub(Vector vec) {
        return new Vector(this.x - vec.x, this.y - vec.y, this.z - vec.z);
    }

    Vector norm() {
        double sum = Math
                .sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return this.sDivide(sum);
    }

    Vector sMult(double A) {
        return new Vector(this.x * A, this.y * A, this.z * A);
    }

    Vector sDivide(double A) {
        return new Vector(this.x / A, this.y / A, this.z / A);
    }

    double mag() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    double vdot(Vector vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }
}

//Ray tracing structure
class Ray {
    //ray = origin + scalar * direction
    //note* scalar*direction is essentially the vector of the ray
    Vector origin, direction;

    Ray(Vector o0, Vector d0) {
        this.origin = o0;
        this.direction = d0.norm();
    }
}

enum Material {
    DIFFUSE, SPECULAR, REFRACTIVE
}

//Object superclass
abstract class Objects {
    Vector color; //eventually replace with wavelength distributions
    double emittance;
    Material type; //surface type @Material
    double refIndex; //refractive index

    Objects() {
        this.color = new Vector(0, 0, 0);
        this.emittance = 0.0;
        this.type = Material.DIFFUSE;
    }

    void setProp(Vector col, double emit, Material typ) {
        this.color = col;
        this.emittance = emit;
        this.type = typ;
    } //set object properties

    abstract double intersect(Ray ray); //object intersections (object dependent)

    abstract Vector vNormal(Vector vec); //surface normal
}

//Spheres
class Sphere extends Objects {
    Vector center;
    double radius;

    Sphere(double r, Vector c) {
        this.center = c;
        this.radius = r;
    }

    @Override
    double intersect(Ray ray) {
        //Sphere intersection can be found in the solution to (vec - cen).(vec - cen) = radius^2
        //(origin + scalar * destination - center).(origin + scalar * destination - center) = radius^2
        //s^2 (direction.direction) + 2(origin-center)direction + (origin-center).(origin-center) = radius^2
        //Solutions for s are indicators for intersection
        double A = 1.0;
        double B = (((ray.origin).vSub(this.center)).sMult(2.0))
                .vdot(ray.direction);
        double C = ((ray.origin).vSub(this.center)).vdot(
                (ray.origin).vSub(this.center)) - this.radius * this.radius;
        double dis = B * B - 4 * A * C;

        //IF dis > 0 there exists two solutions to the intersection equation.
        //IF dis = 0 there exists one solution
        //IF dis < 0 there are no real solutions
        if (dis < 0) {
            return 0;
        } else {
            dis = Math.sqrt(dis);
            double solution1 = (-B - dis) / (2 * A);
            double solution2 = (-B + dis) / (2 * A);
            double minSol = 0.001;
            //solution 1 and 2 are the scalars for the ray's travel.
            //Since a ray will interact at the first intersection, only take the nearest solution (sol > 0).
            //solution1 < solution2 always
            if (solution1 > minSol) {
                return solution1;
            } else if (solution2 > minSol) {
                return solution2;
            }
        }
        return 0;
    }

    @Override
    Vector vNormal(Vector vec) {
        //The normal is calculated relative to where ray intersects the sphere.
        //At this point, the vector being passed in should be the ray which will intersect the surface.
        //Surface - center = radial vector
        return vec.vSub(this.center).norm();
    }
}

//Plane
//At the moment, planes are only defined orthogonoly
//Hopefully, I can redesign these to take any angle, but that may be complicated
class Plane extends Objects {
    Vector normal;
    double pos;

    Plane(double d, Vector n) {
        this.normal = n.norm();
        this.pos = d;
    }

    @Override
    double intersect(Ray ray) {
        double d0 = this.normal.vdot(ray.direction);
        //if ray and plane are not perpendicular
        if (d0 != 0) {
            double s = -1 * (((this.normal.vdot(ray.origin)) + this.pos) / d0);
            double minSol = 0.001;
            if (s > minSol) {
                return s;
            }
        }
        return 0;
    }

    @Override
    Vector vNormal(Vector vec) {
        return this.normal;
    }
}

//Define the actions around an intersection
class Intersection {
    double scalar;
    Objects objHit;

    //Initially, define ray to have no interactions
    Intersection() {
        double inf = Math.pow(10, 6);
        this.scalar = inf;
        this.objHit = null;
    }

    Intersection(double s, Objects obj) {
        this.scalar = s;
        this.objHit = obj;
    }

    boolean miss() {
        return this.objHit == null;
    }
}

//Build environment of spheres and planes
class Environment {
    Set<Objects> Room;

    Environment() {
        this.Room = new HashSet<Objects>();
    }

    void add(Objects obj) {
        this.Room.add(obj);
    }

    Intersection intersecting(Ray ray) {
        Intersection nearest = new Intersection();
        //When testing for intersection, a ray must test all possible objects.
        //Later improvements can be made that take spatial hierarchies into account that limit the number of objects to check.
        Iterator<Objects> i = this.Room.iterator();
        while (i.hasNext()) {
            Objects test = i.next();
            double scalar = test.intersect(ray);
            double minSol = 0.001;
            //Find the nearest intersection
            //Prevent 0's.
            if (scalar > minSol && scalar < nearest.scalar) {
                nearest.scalar = scalar;
                nearest.objHit = test;
            }

        }
        return nearest;
    }
}

//Halton Series
//Pseudo-random numbers that guarantee a balanced distribution
class Halton {
    double value, inv_base;

    Halton(int i, int base) {
        double f = this.inv_base = 1.0 / base;
        this.value = 0.0;
        while (i > 0) {
            this.value += f * (i % base);
            i /= base;
            f = f * base;
        }
    }

    void next() {
        double r = 1.0 - this.value - Math.pow(10, -5);
        if (this.inv_base < r) {
            this.value += this.inv_base;
        } else {
            double h = this.inv_base;
            double hh;
            do {
                hh = h;
                h = h * this.inv_base;
            } while (h >= r);
            this.value += hh + h - 1.0;
        }
    }

    double get() {
        return this.value;
    }
}
