package dev.lumina.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * High-fidelity vector icons for every language and framework in the
 * New Project dialog, matching IntelliJ IDEA Ultimate's visual design.
 */
public final class GeneratorIcons {

    private GeneratorIcons() {
    }

    public static Node getIcon(String label) {
        return switch (label) {
            case "Java" -> javaIcon();
            case "Kotlin" -> kotlinIcon();
            case "Groovy" -> groovyIcon();
            case "Scala" -> scalaIcon();
            case "Python", "Virtualenv" -> pythonIcon();
            case "PHP" -> phpIcon();
            case "Ruby" -> rubyIcon();
            case "Conda" -> condaIcon();
            case "Pipenv" -> pipenvIcon();
            case "Poetry" -> poetryIcon();
            case "uv" -> uvIconSmall();
            case "Hatch" -> hatchIcon();
            case "Rust" -> rustIcon();
            case "Empty Project" -> emptyProjectIcon();
            case "Maven Archetype" -> mavenIcon();
            case "Spring Boot" -> springBootIcon();
            case "JavaFX" -> javafxIcon();
            case "Quarkus" -> quarkusIcon();
            case "Micronaut" -> micronautIcon();
            case "Jakarta EE" -> jakartaIcon();
            case "Ktor" -> ktorIcon();
            case "HTML" -> htmlIcon();
            case "React" -> reactIcon();
            case "Express" -> expressIcon();
            case "Angular CLI" -> angularIcon();
            case "Vue.js" -> vueIcon();
            case "Vite" -> viteIcon();
            case "Nuxt" -> nuxtIcon();
            default -> defaultIcon();
        };
    }

    private static StackPane box(Node... children) {
        StackPane sp = new StackPane(children);
        sp.setMinSize(18, 18);
        sp.setPrefSize(18, 18);
        sp.setMaxSize(18, 18);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    /** Java: warm orange/amber coffee cup with steam rising. */
    private static Node javaIcon() {
        SVGPath saucer = new SVGPath();
        saucer.setContent("M 2,13.5 L 14,13.5");
        saucer.setStroke(Color.web("#E58C35"));
        saucer.setStrokeWidth(1.2);

        SVGPath cup = new SVGPath();
        cup.setContent("M 3,6 L 12,6 C 12,6 11.6,11.5 7.5,11.5 C 3.4,11.5 3,6 3,6 Z");
        cup.setFill(Color.web("#E58C35"));

        SVGPath handle = new SVGPath();
        handle.setContent("M 11.5,7 C 13.5,7 14.5,7.8 14.5,8.8 C 14.5,9.8 13.5,10.5 11.5,10.5");
        handle.setStroke(Color.web("#E58C35"));
        handle.setStrokeWidth(1.2);
        handle.setFill(null);

        SVGPath steam = new SVGPath();
        steam.setContent("M 5.5,1.5 C 6.5,2.7 5.2,3.7 6.2,5 M 9,1 C 10,2.3 8.7,3.3 9.7,4.6");
        steam.setStroke(Color.web("#F5A623"));
        steam.setStrokeWidth(1.0);
        steam.setFill(null);

        Group g = new Group(saucer, cup, handle, steam);
        return box(g);
    }

    /** Kotlin: official purple/magenta/orange diagonal geometric flag. */
    private static Node kotlinIcon() {
        Polygon top = new Polygon(1, 1, 14, 1, 7.5, 7.5);
        top.setFill(Color.web("#7F52FF"));

        Polygon left = new Polygon(1, 1, 7.5, 7.5, 1, 14);
        left.setFill(Color.web("#C757BC"));

        Polygon bottom = new Polygon(7.5, 7.5, 14, 14, 1, 14);
        bottom.setFill(Color.web("#E24A4A"));

        Group g = new Group(top, left, bottom);
        return box(g);
    }

    /** Groovy: rounded green square badge with bold white 'G'. */
    private static Node groovyIcon() {
        Rectangle bg = new Rectangle(15, 15);
        bg.setArcWidth(5);
        bg.setArcHeight(5);
        bg.setFill(Color.web("#4B8E54"));

        Text text = new Text("G");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));

        return box(bg, text);
    }

    /** Rust: circular white gear cog with center hole. */
    private static Node rustIcon() {
        Circle outer = new Circle(6.8);
        outer.setFill(Color.web("#14161E"));
        outer.setStroke(Color.web("#DCDFE6"));
        outer.setStrokeWidth(1.6);

        Circle center = new Circle(2.6);
        center.setFill(Color.web("#14161E"));
        center.setStroke(Color.web("#DCDFE6"));
        center.setStrokeWidth(1.2);

        Group teeth = new Group();
        for (int i = 0; i < 6; i++) {
            Rectangle tooth = new Rectangle(2.4, 2.0);
            tooth.setFill(Color.web("#DCDFE6"));
            tooth.setX(-1.2);
            tooth.setY(-7.8);
            tooth.setRotate(i * 60);
            teeth.getChildren().add(tooth);
        }

        Group g = new Group(outer, teeth, center);
        return box(g);
    }

    /** Empty Project: outline folder with sparkle star. */
    private static Node emptyProjectIcon() {
        SVGPath folder = new SVGPath();
        folder.setContent("M 2,4 L 6,4 L 7.5,5.5 L 14.5,5.5 L 14.5,13 L 2,13 Z");
        folder.setStroke(Color.web("#9AA0A6"));
        folder.setStrokeWidth(1.2);
        folder.setFill(null);

        SVGPath star = new SVGPath();
        star.setContent("M 12.5,1.5 L 13.1,2.7 L 14.5,3.3 L 13.1,3.9 L 12.5,5.2 L 11.9,3.9 L 10.5,3.3 L 11.9,2.7 Z");
        star.setFill(Color.web("#61AFEF"));

        Group g = new Group(folder, star);
        return box(g);
    }

    /** Maven Archetype: classic italic bright blue 'm'. */
    private static Node mavenIcon() {
        Text m = new Text("m");
        m.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, 14));
        m.setFill(Color.web("#3B82F6"));
        return box(m);
    }

    /** Spring Boot: vibrant spring green leaf with center vein. */
    private static Node springBootIcon() {
        SVGPath leaf = new SVGPath();
        leaf.setContent("M 2,14 C 2,14 1,5.5 7.5,1.5 C 13,-2 15,1 15,1 C 15,1 17,7 11.5,12.5 C 6,17 2,14 2,14 Z");
        leaf.setFill(Color.web("#6DB33F"));

        SVGPath vein = new SVGPath();
        vein.setContent("M 2,14 C 5,10.5 8,7 13.5,2");
        vein.setStroke(Color.WHITE);
        vein.setStrokeWidth(1.1);
        vein.setFill(null);

        Group g = new Group(leaf, vein);
        return box(g);
    }

    /** JavaFX: desktop window outline with header line. */
    private static Node javafxIcon() {
        Rectangle rect = new Rectangle(14, 11);
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        rect.setStroke(Color.web("#A8B2C4"));
        rect.setStrokeWidth(1.2);
        rect.setFill(null);

        SVGPath line = new SVGPath();
        line.setContent("M 2,4.5 L 14,4.5");
        line.setStroke(Color.web("#A8B2C4"));
        line.setStrokeWidth(1.0);

        Group g = new Group(rect, line);
        return box(g);
    }

    /** Quarkus: blue badge with white Quarkus loop/shield. */
    private static Node quarkusIcon() {
        Rectangle bg = new Rectangle(15, 15);
        bg.setArcWidth(5);
        bg.setArcHeight(5);
        bg.setFill(Color.web("#4695EB"));

        SVGPath loop = new SVGPath();
        loop.setContent("M 7.5,3 C 9.8,3 11.5,4.8 11.5,7 C 11.5,8.4 10.8,9.6 9.8,10.3 L 11.5,12 M 7.5,11 C 5.2,11 3.5,9.2 3.5,7 C 3.5,4.8 5.2,3 7.5,3 Z");
        loop.setStroke(Color.WHITE);
        loop.setStrokeWidth(1.3);
        loop.setFill(null);

        Circle dot = new Circle(1.2, Color.web("#00E5FF"));
        dot.setTranslateX(2);
        dot.setTranslateY(-2);

        Group g = new Group(bg, loop, dot);
        return box(g);
    }

    /** Micronaut: Greek letter mu in italic serif. */
    private static Node micronautIcon() {
        Text mu = new Text("μ");
        mu.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, 14.5));
        mu.setFill(Color.web("#D8DBE6"));
        return box(mu);
    }

    /** Jakarta EE: red-orange rising sun / semicircle. */
    private static Node jakartaIcon() {
        SVGPath sun = new SVGPath();
        sun.setContent("M 2.5,10 A 5,5 0 0,1 12.5,10 Z M 1,11 L 14,11 M 7.5,1.5 L 7.5,3.5 M 3,3.5 L 4.5,4.8 M 12,3.5 L 10.5,4.8");
        sun.setStroke(Color.web("#F05032"));
        sun.setStrokeWidth(1.2);
        sun.setFill(Color.web("#F05032"));
        return box(sun);
    }

    /** Ktor: purple/magenta geometric diamond blocks. */
    private static Node ktorIcon() {
        Polygon p1 = new Polygon(7.5, 1, 14, 7.5, 7.5, 7.5);
        p1.setFill(Color.web("#7F52FF"));

        Polygon p2 = new Polygon(1, 7.5, 7.5, 7.5, 7.5, 14);
        p2.setFill(Color.web("#C757BC"));

        Polygon p3 = new Polygon(7.5, 7.5, 14, 14, 1, 14);
        p3.setFill(Color.web("#E24A4A"));

        Group ktor = new Group(p1, p2, p3);
        return box(ktor);
    }

    /** HTML: orange HTML5 shield with white '5'. */
    private static Node htmlIcon() {
        SVGPath shield = new SVGPath();
        shield.setContent("M 2,1.5 L 13,1.5 L 12,12 L 7.5,13.5 L 3,12 Z");
        shield.setFill(Color.web("#E44D26"));

        Text t = new Text("5");
        t.setFont(Font.font("sans-serif", FontWeight.BOLD, 8.5));
        t.setFill(Color.WHITE);

        return box(shield, t);
    }

    /** React: cyan atomic orbital rings with central nucleus. */
    private static Node reactIcon() {
        Circle center = new Circle(1.4, Color.web("#61DAFB"));

        Ellipse e1 = new Ellipse(6.5, 2.3);
        e1.setStroke(Color.web("#61DAFB"));
        e1.setFill(null);
        e1.setStrokeWidth(0.9);

        Ellipse e2 = new Ellipse(6.5, 2.3);
        e2.setStroke(Color.web("#61DAFB"));
        e2.setFill(null);
        e2.setStrokeWidth(0.9);
        e2.setRotate(60);

        Ellipse e3 = new Ellipse(6.5, 2.3);
        e3.setStroke(Color.web("#61DAFB"));
        e3.setFill(null);
        e3.setStrokeWidth(0.9);
        e3.setRotate(120);

        return box(e1, e2, e3, center);
    }

    /** Express: clean bold lowercase 'ex' in light grey. */
    private static Node expressIcon() {
        Text ex = new Text("ex");
        ex.setFont(Font.font("sans-serif", FontWeight.BOLD, 10.5));
        ex.setFill(Color.web("#B9BECF"));
        return box(ex);
    }

    /** Angular CLI: crimson shield with white 'A'. */
    private static Node angularIcon() {
        SVGPath shield = new SVGPath();
        shield.setContent("M 7.5,1 L 13.5,3.2 L 12.5,11 L 7.5,13.8 L 2.5,11 L 1.5,3.2 Z");
        shield.setFill(Color.web("#DD0031"));

        Text a = new Text("A");
        a.setFont(Font.font("sans-serif", FontWeight.BOLD, 8.5));
        a.setFill(Color.WHITE);

        return box(shield, a);
    }

    /** Vue.js: emerald green outer 'V' with navy inner 'V'. */
    private static Node vueIcon() {
        Polygon outer = new Polygon(1, 2, 14, 2, 7.5, 13.5);
        outer.setFill(Color.web("#42B883"));

        Polygon inner = new Polygon(4.5, 2, 10.5, 2, 7.5, 7.5);
        inner.setFill(Color.web("#35495E"));

        Group vue = new Group(outer, inner);
        return box(vue);
    }

    /** Vite: purple 'V' with bright yellow lightning bolt. */
    private static Node viteIcon() {
        Polygon v = new Polygon(1.5, 2, 13.5, 2, 7.5, 13.5);
        v.setFill(Color.web("#646CFF"));

        SVGPath bolt = new SVGPath();
        bolt.setContent("M 8.5,1 L 5.5,6.5 L 8,6.5 L 6.8,11.5 L 10.5,5.5 L 8,5.5 Z");
        bolt.setFill(Color.web("#FFD62E"));

        Group vite = new Group(v, bolt);
        return box(vite);
    }

    /** Nuxt: emerald green overlapping mountain triangles. */
    private static Node nuxtIcon() {
        Polygon m1 = new Polygon(1, 12, 6, 3.5, 11, 12);
        m1.setFill(Color.web("#00DC82"));

        Polygon m2 = new Polygon(5.5, 12, 9.5, 5, 13.5, 12);
        m2.setFill(Color.web("#00C58E"));
        m2.setStroke(Color.web("#002E3B"));
        m2.setStrokeWidth(0.8);

        Group nuxt = new Group(m1, m2);
        return box(nuxt);
    }

    /** Scala: red spiral staircase logo matching IntelliJ IDEA. */
    private static Node scalaIcon() {
        SVGPath scala = new SVGPath();
        scala.setContent("M 12.37 0.75 c 0.00 1.15 -5.56 2.04 -8.75 2.25 v 3.28 c 1.35 0.09 3.12 0.30 4.71 0.60 c -1.60 0.30 -3.36 0.51 -4.71 0.60 v 3.28 c 1.35 0.09 3.11 0.30 4.71 0.60 c -1.59 0.30 -3.36 0.51 -4.71 0.60 V 15.25 l 1.58 -0.23 c 2.95 -0.44 6.20 -1.28 7.17 -2.02 V 9.69 c 0.00 -0.21 -0.38 -0.43 -0.98 -0.66 c 0.43 -0.17 0.77 -0.34 0.98 -0.51 V 5.22 c 0.00 -0.21 -0.37 -0.43 -0.97 -0.66 c 0.42 -0.17 0.76 -0.35 0.97 -0.51 V 0.75 z M 11.39 9.39 c 0.44 0.17 0.59 0.29 0.64 0.34 c -0.04 0.07 -0.17 0.21 -0.55 0.40 c -0.06 0.03 -0.13 0.06 -0.19 0.08 l 0.00 0.00 c -0.65 0.28 -1.67 0.56 -2.97 0.80 c -0.64 -0.12 -1.32 -0.23 -2.01 -0.32 C 8.26 10.35 10.17 9.86 11.39 9.39 z M 11.40 4.92 c 0.43 0.17 0.58 0.29 0.63 0.34 c -0.03 0.05 -0.11 0.15 -0.32 0.28 l -0.00 -0.00 c -0.04 0.02 -0.09 0.05 -0.14 0.08 c -0.62 0.33 -1.74 0.65 -3.22 0.93 c -0.64 -0.12 -1.33 -0.23 -2.01 -0.32 C 8.28 5.88 10.17 5.40 11.40 4.92 z");
        scala.setFill(Color.web("#DE3423"));
        return box(scala);
    }

    /** Python: official blue and yellow intertwined snakes matching IntelliJ IDEA. */
    public static Node pythonIcon() {
        SVGPath blueSnake = new SVGPath();
        blueSnake.setContent("M 7.94 1.05 c -3.55 0.00 -3.33 1.53 -3.33 1.53 V 4.18 h 3.38 v 0.50 H 3.27 S 1.00 4.40 1.00 8.00 s 2.01 3.46 2.01 3.46 H 4.17 V 9.78 s -0.07 -2.01 1.95 -2.01 h 3.38 s 1.89 0.03 1.89 -1.83 V 2.90 s 0.29 -1.86 -3.42 -1.86 h 0.00 Z M 6.08 2.12 a 0.61 0.61 0 1 1 -0.59 0.62 v -0.01 a 0.61 0.61 0 0 1 0.61 -0.61 h 0.00 Z");
        blueSnake.setFill(Color.web("#3776AB"));

        SVGPath yellowSnake = new SVGPath();
        yellowSnake.setContent("M 8.04 14.96 c 3.55 0.00 3.33 -1.54 3.33 -1.54 V 11.82 H 7.99 v -0.50 h 4.74 S 15.00 11.58 15.00 8.00 s -2.01 -3.46 -2.01 -3.46 H 11.82 V 6.20 s 0.07 2.01 -1.95 2.01 H 6.49 S 4.60 8.18 4.60 10.03 V 13.10 s -0.29 1.86 3.42 1.86 h 0.02 Z m 1.87 -1.07 a 0.61 0.61 0 1 1 0.59 -0.62 v 0.01 a 0.61 0.61 0 0 1 -0.61 0.61 h 0.01 Z");
        yellowSnake.setFill(Color.web("#FFD43B"));

        Group g = new Group(blueSnake, yellowSnake);
        return box(g);
    }

    /** Conda: official green segmented ring matching IntelliJ IDEA. */
    public static Node condaIcon() {
        Circle ring = new Circle(6.5);
        ring.setFill(null);
        ring.setStroke(Color.web("#43B02A"));
        ring.setStrokeWidth(2.2);

        // Segment cutout dot
        Circle dot = new Circle(1.2);
        dot.setFill(Color.web("#43B02A"));
        dot.setTranslateX(4.5);
        dot.setTranslateY(-4.5);

        Group g = new Group(ring, dot);
        return box(g);
    }

    /** Pipenv: blue/cyan chemical flask/beaker matching IntelliJ IDEA. */
    public static Node pipenvIcon() {
        SVGPath flask = new SVGPath();
        flask.setContent("M 6.5,1.5 L 9.5,1.5 L 9.5,4.5 L 13.5,11.5 C 14.2,12.7 13.4,14.5 12,14.5 L 4,14.5 C 2.6,14.5 1.8,12.7 2.5,11.5 L 6.5,4.5 Z");
        flask.setFill(Color.web("#2B88D9"));

        SVGPath liquid = new SVGPath();
        liquid.setContent("M 4,14.5 L 12,14.5 L 11,11 C 9.5,11.5 6.5,10.5 5,11 Z");
        liquid.setFill(Color.web("#68B9ED"));

        Group g = new Group(flask, liquid);
        return box(g);
    }

    /** Poetry: cyan and blue origami paper airplane matching IntelliJ IDEA. */
    public static Node poetryIcon() {
        Polygon leftWing = new Polygon(8, 1.5, 2, 12, 8, 9.5);
        leftWing.setFill(Color.web("#00ADEF"));

        Polygon rightWing = new Polygon(8, 1.5, 14, 12, 8, 9.5);
        rightWing.setFill(Color.web("#0084FF"));

        Polygon keel = new Polygon(8, 9.5, 8, 14.5, 6, 12);
        keel.setFill(Color.web("#0057B8"));

        Group g = new Group(leftWing, rightWing, keel);
        return box(g);
    }

    /** uv: purple/magenta modern badge matching IntelliJ IDEA. */
    public static Node uvIconSmall() {
        Rectangle bg = new Rectangle(14, 14);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        bg.setFill(Color.web("#8A2BE2"));

        Text uvText = new Text("uv");
        uvText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.5));
        uvText.setFill(Color.WHITE);

        return box(bg, uvText);
    }

    /** Hatch: stylish hatch bird/hexagon icon matching IntelliJ IDEA. */
    public static Node hatchIcon() {
        Polygon hex = new Polygon(
                8, 1.5,
                13.5, 4.5,
                13.5, 11.5,
                8, 14.5,
                2.5, 11.5,
                2.5, 4.5
        );
        hex.setFill(Color.web("#198754"));

        SVGPath bird = new SVGPath();
        bird.setContent("M 5.5,10 L 8,5.5 L 10.5,10 L 9,10 L 8,8 L 7,10 Z");
        bird.setFill(Color.WHITE);

        Group g = new Group(hex, bird);
        return box(g);
    }

    /** PHP: official indigo/purple oval with white italic 'php' text matching IntelliJ IDEA. */
    public static Node phpIcon() {
        Ellipse oval = new Ellipse(7.5, 5.0);
        oval.setFill(Color.web("#777BB4"));

        Text text = new Text("php");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, FontPosture.ITALIC, 7.5));

        StackPane pane = new StackPane(oval, text);
        pane.setAlignment(Pos.CENTER);
        return box(pane);
    }

    /** Ruby: official red faceted gemstone matching IntelliJ IDEA. */
    public static Node rubyIcon() {
        // Table & crown (upper trapezoid)
        Polygon crown = new Polygon(
                4.0, 3.0,
                12.0, 3.0,
                14.5, 7.0,
                1.5, 7.0
        );
        crown.setFill(Color.web("#EF5350"));

        // Pavilion (lower triangle pointing to bottom)
        Polygon pavilion = new Polygon(
                1.5, 7.0,
                14.5, 7.0,
                8.0, 14.0
        );
        pavilion.setFill(Color.web("#C62828"));

        // Center table facet
        Polygon centerFacet = new Polygon(
                5.5, 3.0,
                10.5, 3.0,
                11.5, 7.0,
                4.5, 7.0
        );
        centerFacet.setFill(Color.web("#FF7961"));

        // Bottom center facet
        Polygon centerLower = new Polygon(
                4.5, 7.0,
                11.5, 7.0,
                8.0, 14.0
        );
        centerLower.setFill(Color.web("#B71C1C"));

        Group g = new Group(crown, pavilion, centerFacet, centerLower);
        return box(g);
    }

    /** Default fallback bullet. */
    private static Node defaultIcon() {
        Circle c = new Circle(3.5, Color.web("#8B92A6"));
        return box(c);
    }
}
