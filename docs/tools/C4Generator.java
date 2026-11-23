import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Генератор архитектурных схем C4 для проекта «Прокат спортивного инвентаря».
 *
 * Из одного описания строит:
 *   docs/architecture-context.png
 *   docs/architecture-container.png
 *   docs/architecture-component.png
 *   docs/architecture.png          (все три уровня одним листом)
 *   docs/architecture.drawio       (редактируемый файл, 3 страницы)
 *
 * Запуск из корня репозитория:  java docs/tools/C4Generator.java
 */
public class C4Generator {

    static final int W = 1400;
    static final int H = 900;

    static final String FONT = "Segoe UI";

    enum Kind {PERSON, CONTAINER, DATABASE, EXTERNAL, BOUNDARY}

    record Node(String id, int x, int y, int w, int h, String title, List<String> lines, Kind kind) {
    }

    record Edge(String from, String to, String label, List<Point> via, Point labelAt, boolean jdbc) {
    }

    record Page(String file, String diagramName, String heading, String subheading,
                List<Node> nodes, List<Edge> edges, List<String> notes,
                int noteX, int noteY, int legendX, int legendY) {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("docs"));
        List<Page> pages = pages();

        BufferedImage combined = new BufferedImage(W, H * pages.size(), BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = combined.createGraphics();
        cg.setColor(Color.WHITE);
        cg.fillRect(0, 0, combined.getWidth(), combined.getHeight());

        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            BufferedImage image = render(page);
            ImageIO.write(image, "png", Path.of("docs", page.file()).toFile());
            cg.drawImage(image, 0, H * i, null);
            System.out.println("written docs/" + page.file());
        }
        cg.dispose();
        ImageIO.write(combined, "png", Path.of("docs", "architecture.png").toFile());
        System.out.println("written docs/architecture.png");

        Files.writeString(Path.of("docs", "architecture.drawio"), drawio(pages), StandardCharsets.UTF_8);
        System.out.println("written docs/architecture.drawio");
    }

    // ------------------------------------------------------------------
    // Описание схем
    // ------------------------------------------------------------------

    static List<Page> pages() {
        return List.of(contextPage(), containerPage(), componentPage());
    }

    static Page contextPage() {
        List<Node> nodes = List.of(
                new Node("client", 120, 300, 230, 120, "Клиент пункта проката",
                        List.of("[Person]", "Берёт инвентарь в аренду"), Kind.PERSON),
                new Node("admin", 120, 540, 230, 110, "Администратор",
                        List.of("[Person]", "Ведёт каталог инвентаря"), Kind.PERSON),
                new Node("system", 570, 280, 360, 190, "Система проката\nспортинвентаря",
                        List.of("[Software System]", "Оформление и возврат аренды,",
                                "учёт доступного инвентаря"), Kind.CONTAINER),
                new Node("channel", 1070, 300, 280, 140, "Канал уведомлений",
                        List.of("[External System]", "Имитация: журнал", "уведомлений в консоли"), Kind.EXTERNAL)
        );
        List<Edge> edges = List.of(
                new Edge("client", "system",
                        "Оформляет и возвращает аренду", List.of(), new Point(455, 330), false),
                new Edge("admin", "system",
                        "Добавляет и сопровождает инвентарь", List.of(), null, false),
                new Edge("system", "channel",
                        "События аренды (HTTP, запись в лог)", List.of(), null, false)
        );
        return new Page("architecture-context.png", "C4 Context",
                "C4 Level 1 — System Context",
                "Кто пользуется системой проката и с чем она взаимодействует снаружи",
                nodes, edges,
                List.of("PostgreSQL и микросервисы — внутреннее устройство системы, они показаны на уровне Container."),
                60, 710, 60, 820);
    }

    static Page containerPage() {
        List<Node> nodes = List.of(
                new Node("client", 60, 380, 190, 100, "Клиент",
                        List.of("[Person]", "Пользователь API"), Kind.PERSON),
                new Node("gateway", 300, 365, 225, 120, "API Gateway",
                        List.of("[Container: Spring Boot]", "Маршрутизация /api/**"), Kind.CONTAINER),
                new Node("users", 620, 90, 250, 85, "users-service",
                        List.of("[Container: Spring Boot]", "Пользователи"), Kind.CONTAINER),
                new Node("equipment", 620, 220, 250, 85, "equipment-service",
                        List.of("[Container: Spring Boot]", "Каталог и остатки"), Kind.CONTAINER),
                new Node("notifications", 620, 350, 250, 85, "notifications-service",
                        List.of("[Container: Spring Boot]", "Уведомления (лог)"), Kind.CONTAINER),
                new Node("rentals", 620, 480, 250, 85, "rentals-service",
                        List.of("[Container: Spring Boot]", "Аренда (оркестратор)"), Kind.CONTAINER),
                new Node("db", 1060, 270, 270, 140, "PostgreSQL 16",
                        List.of("[Container: postgres:16]", "БД app, volume db_data",
                                "4 схемы по сервисам"), Kind.DATABASE)
        );
        List<Edge> edges = List.of(
                // сначала пунктирные подключения к БД, чтобы оранжевые линии читались поверх
                new Edge("users", "db", null, List.of(), null, true),
                new Edge("equipment", "db", null, List.of(), null, true),
                new Edge("notifications", "db", null, List.of(), null, true),
                new Edge("rentals", "db", null, List.of(), null, true),

                new Edge("client", "gateway", "HTTPS / JSON", List.of(), new Point(275, 338), false),
                new Edge("gateway", "users", "HTTP", List.of(), null, false),
                new Edge("gateway", "equipment", "HTTP", List.of(), null, false),
                new Edge("gateway", "notifications", "HTTP", List.of(), null, false),
                new Edge("gateway", "rentals", "HTTP", List.of(), null, false),

                new Edge("rentals", "notifications", "HTTP: уведомление",
                        List.of(), new Point(745, 457), false),
                new Edge("rentals", "equipment", "HTTP: reserve / release",
                        List.of(new Point(1000, 522), new Point(1000, 262)), new Point(940, 502), false),
                new Edge("rentals", "users", "HTTP: проверить пользователя",
                        List.of(new Point(1020, 522), new Point(1020, 132)), new Point(965, 112), false)
        );
        return new Page("architecture-container.png", "C4 Container",
                "C4 Level 2 — Containers",
                "Сервисы, API Gateway и PostgreSQL: как контейнеры общаются между собой",
                nodes, edges,
                List.of("Пунктир — подключение сервиса к своей схеме в БД app через JDBC.",
                        "Штатный вход — API Gateway (/api/**); порты сервисов опубликованы только для отладки.",
                        "Сценарий: POST /api/rentals -> rentals-service -> users-service + equipment-service + PostgreSQL + notifications-service."),
                60, 690, 60, 820);
    }

    static Page componentPage() {
        List<Node> nodes = List.of(
                new Node("boundary", 200, 115, 780, 520, "Контейнер rentals-service",
                        List.of(), Kind.BOUNDARY),
                new Node("controller", 250, 155, 220, 80, "RentalController",
                        List.of("REST /rentals"), Kind.CONTAINER),
                new Node("service", 250, 300, 220, 90, "RentalService",
                        List.of("Бизнес-логика аренды"), Kind.CONTAINER),
                new Node("repository", 250, 480, 220, 80, "RentalRepository",
                        List.of("JdbcTemplate / SQL"), Kind.CONTAINER),
                new Node("userClient", 600, 155, 220, 70, "UserServiceClient",
                        List.of("RestClient"), Kind.CONTAINER),
                new Node("equipmentClient", 600, 300, 220, 70, "EquipmentServiceClient",
                        List.of("RestClient"), Kind.CONTAINER),
                new Node("notificationClient", 600, 480, 220, 70, "NotificationServiceClient",
                        List.of("RestClient"), Kind.CONTAINER),
                new Node("users", 1080, 155, 240, 70, "users-service",
                        List.of("[Container]"), Kind.EXTERNAL),
                new Node("equipment", 1080, 300, 240, 70, "equipment-service",
                        List.of("[Container]"), Kind.EXTERNAL),
                new Node("notifications", 1080, 480, 240, 70, "notifications-service",
                        List.of("[Container]"), Kind.EXTERNAL),
                new Node("db", 250, 700, 310, 115, "PostgreSQL 16",
                        List.of("[Container]", "schema rentals_service,", "таблица rentals"), Kind.DATABASE)
        );
        List<Edge> edges = List.of(
                new Edge("controller", "service", null, List.of(), null, false),
                new Edge("service", "repository", "save / update", List.of(), null, false),
                new Edge("repository", "db", "SQL", List.of(), new Point(405, 665), false),
                new Edge("service", "userClient", null,
                        List.of(new Point(550, 345), new Point(550, 190)), null, false),
                new Edge("service", "equipmentClient", null,
                        List.of(new Point(550, 345), new Point(550, 335)), null, false),
                new Edge("service", "notificationClient", null,
                        List.of(new Point(550, 345), new Point(550, 515)), null, false),
                new Edge("userClient", "users", "HTTP", List.of(), null, false),
                new Edge("equipmentClient", "equipment", "HTTP", List.of(), null, false),
                new Edge("notificationClient", "notifications", "HTTP", List.of(), null, false)
        );
        return new Page("architecture-component.png", "C4 Component",
                "C4 Level 3 — Components: rentals-service",
                "Из каких компонентов состоит главный сервис и как он вызывает остальные",
                nodes, edges,
                List.of("RentalService — оркестратор: проверяет пользователя и остаток, бронирует инвентарь,",
                        "сохраняет аренду в PostgreSQL и отправляет уведомление. Ошибка уведомления не отменяет аренду."),
                610, 700, 610, 790);
    }

    // ------------------------------------------------------------------
    // Рендер PNG
    // ------------------------------------------------------------------

    static BufferedImage render(Page page) {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        g.setFont(new Font(FONT, Font.BOLD, 28));
        g.setColor(new Color(0x1F2933));
        g.drawString(page.heading(), 60, 62);
        g.setFont(new Font(FONT, Font.PLAIN, 16));
        g.setColor(new Color(0x52606D));
        g.drawString(page.subheading(), 60, 94);

        for (Node node : page.nodes()) {
            if (node.kind() == Kind.BOUNDARY) {
                drawBoundary(g, node);
            }
        }

        for (Edge edge : page.edges()) {
            drawEdge(g, page, edge);
        }

        for (Node node : page.nodes()) {
            if (node.kind() != Kind.BOUNDARY) {
                drawNode(g, node);
            }
        }

        for (Edge edge : page.edges()) {
            drawEdgeLabel(g, page, edge);
        }

        int noteY = page.noteY();
        g.setFont(new Font(FONT, Font.PLAIN, 14));
        for (String note : page.notes()) {
            g.setColor(new Color(0x52606D));
            g.drawString(note, page.noteX(), noteY);
            noteY += 24;
        }

        drawLegend(g, page.legendX(), page.legendY());
        g.setFont(new Font(FONT, Font.PLAIN, 12));
        g.setColor(new Color(0x9AA4AE));
        g.drawString("Схема сгенерирована docs/tools/C4Generator.java • редактируемая версия: docs/architecture.drawio", 60, 880);

        g.dispose();
        return image;
    }

    static void drawLegend(Graphics2D g, int x, int y) {
        int cx = x;
        cx = legendItem(g, cx, y, fillFor(Kind.PERSON), false, "Person");
        cx = legendItem(g, cx, y, fillFor(Kind.CONTAINER), false, "Container (Spring Boot)");
        cx = legendItem(g, cx, y, fillFor(Kind.DATABASE), false, "Database");
        cx = legendItem(g, cx, y, fillFor(Kind.EXTERNAL), false, "External system");
        legendItem(g, cx, y, new Color(0xC2681B), false, "HTTP-взаимодействие");
    }

    static int legendItem(Graphics2D g, int x, int y, Color color, boolean dashed, String label) {
        g.setColor(color);
        g.fillRoundRect(x, y - 12, 26, 16, 5, 5);
        g.setFont(new Font(FONT, Font.PLAIN, 12));
        g.setColor(new Color(0x3E4C59));
        g.drawString(label, x + 34, y);
        return x + 34 + g.getFontMetrics().stringWidth(label) + 34;
    }

    static Color fillFor(Kind kind) {
        return switch (kind) {
            case PERSON -> new Color(0x083D6B);
            case CONTAINER -> new Color(0x1168BD);
            case DATABASE -> new Color(0x1B6D4A);
            case EXTERNAL -> new Color(0x8C8C8C);
            case BOUNDARY -> Color.WHITE;
        };
    }

    static void drawNode(Graphics2D g, Node node) {
        Color fill = fillFor(node.kind());
        g.setColor(new Color(0, 0, 0, 28));
        g.fillRoundRect(node.x() + 2, node.y() + 4, node.w(), node.h(), 16, 16);
        g.setColor(fill);
        g.fillRoundRect(node.x(), node.y(), node.w(), node.h(), 16, 16);

        int lines = node.lines().size();
        int blockHeight = 24 + lines * 20;
        int titleY = node.y() + (node.h() - blockHeight) / 2 + 18;
        int cx = node.x() + node.w() / 2;

        String[] titleLines = node.title().split("\n");
        int ty = titleY;
        for (String line : titleLines) {
            drawCentered(g, line, cx, ty, new Font(FONT, Font.BOLD, 16), Color.WHITE);
            ty += 20;
        }
        ty = titleY + titleLines.length * 20 + 2;
        for (String line : node.lines()) {
            drawCentered(g, line, cx, ty, new Font(FONT, Font.PLAIN, 12), new Color(0xE8F0F8));
            ty += 20;
        }
    }

    static void drawBoundary(Graphics2D g, Node node) {
        g.setColor(new Color(0x9AA4AE));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{7f, 6f}, 0f));
        g.drawRoundRect(node.x(), node.y(), node.w(), node.h(), 12, 12);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font(FONT, Font.BOLD, 13));
        g.setColor(new Color(0x52606D));
        g.drawString(node.title(), node.x() + 14, node.y() + 24);
    }

    static void drawCentered(Graphics2D g, String text, int cx, int baseline, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
    }

    static void drawEdge(Graphics2D g, Page page, Edge edge) {
        Node from = find(page, edge.from());
        Node to = find(page, edge.to());

        List<Point> points = new ArrayList<>();
        Point firstTarget = edge.via().isEmpty() ? center(to) : edge.via().get(0);
        Point lastSource = edge.via().isEmpty() ? center(from) : edge.via().get(edge.via().size() - 1);
        points.add(borderPoint(from, firstTarget));
        points.addAll(edge.via());
        points.add(borderPoint(to, lastSource));

        Color color = edge.jdbc() ? new Color(0x5B7FA6) : new Color(0xC2681B);
        if (isNeutral(edge)) {
            color = new Color(0x6B7785);
        }

        if (!edge.jdbc() && !isNeutral(edge)) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawPolyline(g, points);
        }

        g.setColor(color);
        if (edge.jdbc()) {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{6f, 5f}, 0f));
        } else {
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        drawPolyline(g, points);
        g.setStroke(new BasicStroke(1f));
        drawArrowHead(g, points.get(points.size() - 2), points.get(points.size() - 1), color);
    }

    static boolean isNeutral(Edge edge) {
        // Gateway -> сервисы и контроллер -> сервис: технические вызовы без акцента
        return edge.label() != null && edge.label().equals("HTTP")
                || edge.from().equals("controller") && edge.to().equals("service")
                || edge.from().equals("service") && edge.to().equals("repository");
    }

    static void drawPolyline(Graphics2D g, List<Point> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);
            g.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    static void drawArrowHead(Graphics2D g, Point from, Point to, Color color) {
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int size = 11;
        int x1 = (int) Math.round(to.x - size * Math.cos(angle - Math.PI / 7));
        int y1 = (int) Math.round(to.y - size * Math.sin(angle - Math.PI / 7));
        int x2 = (int) Math.round(to.x - size * Math.cos(angle + Math.PI / 7));
        int y2 = (int) Math.round(to.y - size * Math.sin(angle + Math.PI / 7));
        g.setColor(color);
        g.fillPolygon(new int[]{to.x, x1, x2}, new int[]{to.y, y1, y2}, 3);
    }

    static void drawEdgeLabel(Graphics2D g, Page page, Edge edge) {
        if (edge.label() == null || edge.label().isBlank()) {
            return;
        }
        Point position = edge.labelAt();
        if (position == null) {
            List<Point> points = new ArrayList<>();
            Point firstTarget = edge.via().isEmpty() ? center(find(page, edge.to())) : edge.via().get(0);
            Point lastSource = edge.via().isEmpty() ? center(find(page, edge.from()))
                    : edge.via().get(edge.via().size() - 1);
            points.add(borderPoint(find(page, edge.from()), firstTarget));
            points.addAll(edge.via());
            points.add(borderPoint(find(page, edge.to()), lastSource));
            position = midpointOfLongestSegment(points);
        }

        Font font = new Font(FONT, Font.PLAIN, 11);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(edge.label());
        int boxWidth = textWidth + 12;
        int boxHeight = fm.getHeight() + 2;
        g.setColor(new Color(255, 255, 255));
        g.fillRoundRect(position.x - boxWidth / 2, position.y - boxHeight / 2 + 1, boxWidth, boxHeight, 8, 8);
        g.setColor(new Color(0x8A4B12));
        g.drawString(edge.label(), position.x - textWidth / 2, position.y + fm.getAscent() / 2);
    }

    static Point midpointOfLongestSegment(List<Point> points) {
        double best = -1;
        Point bestPoint = points.get(0);
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i);
            Point b = points.get(i + 1);
            double length = a.distance(b);
            if (length > best) {
                best = length;
                bestPoint = new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
            }
        }
        return bestPoint;
    }

    static Point center(Node node) {
        return new Point(node.x() + node.w() / 2, node.y() + node.h() / 2);
    }

    static Point borderPoint(Node node, Point towards) {
        double cx = node.x() + node.w() / 2.0;
        double cy = node.y() + node.h() / 2.0;
        double dx = towards.x - cx;
        double dy = towards.y - cy;
        if (dx == 0 && dy == 0) {
            return new Point((int) cx, (int) cy);
        }
        double scale = Math.min(
                dx != 0 ? Math.abs((node.w() / 2.0) / dx) : Double.MAX_VALUE,
                dy != 0 ? Math.abs((node.h() / 2.0) / dy) : Double.MAX_VALUE);
        return new Point((int) Math.round(cx + dx * scale), (int) Math.round(cy + dy * scale));
    }

    static Node find(Page page, String id) {
        return page.nodes().stream()
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + id));
    }

    // ------------------------------------------------------------------
    // Экспорт в draw.io (3 страницы)
    // ------------------------------------------------------------------

    static String drawio(List<Page> pages) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<mxfile host=\"app.diagrams.net\" agent=\"C4Generator\" version=\"24.7.7\">\n");
        for (Page page : pages) {
            xml.append("  <diagram id=\"").append(esc(page.diagramName().toLowerCase(Locale.ROOT).replace(' ', '-'))).append("\" name=\"")
                    .append(esc(page.diagramName())).append("\">\n");
            xml.append("    <mxGraphModel dx=\"").append(W).append("\" dy=\"").append(H)
                    .append("\" grid=\"1\" gridSize=\"10\" guides=\"1\" tooltips=\"1\" connect=\"1\" arrows=\"1\" fold=\"1\" page=\"1\" pageScale=\"1\" pageWidth=\"")
                    .append(W).append("\" pageHeight=\"").append(H).append("\" math=\"0\" shadow=\"0\">\n");
            xml.append("      <root>\n");
            xml.append("        <mxCell id=\"0\"/>\n");
            xml.append("        <mxCell id=\"1\" parent=\"0\"/>\n");

            xml.append(textCell("title", 40, 30, 1000, 40, page.heading(), "fontSize=24;fontStyle=1;fontColor=#1F2933;"));
            xml.append(textCell("subtitle", 40, 70, 1200, 30, page.subheading(), "fontSize=14;fontColor=#52606D;"));

            int noteIndex = 0;
            for (String note : page.notes()) {
                xml.append(textCell("note-" + noteIndex, page.noteX(), page.noteY() - 18 + noteIndex * 24, 1300, 24, note,
                        "fontSize=12;fontColor=#52606D;"));
                noteIndex++;
            }

            for (Node node : page.nodes()) {
                xml.append(vertex(page, node));
            }
            for (Edge edge : page.edges()) {
                xml.append(edgeCell(edge));
            }

            xml.append("      </root>\n");
            xml.append("    </mxGraphModel>\n");
            xml.append("  </diagram>\n");
        }
        xml.append("</mxfile>\n");
        return xml.toString();
    }

    static String vertex(Page page, Node node) {
        String rawLabel;
        if (node.kind() == Kind.BOUNDARY) {
            rawLabel = node.title();
        } else {
            StringBuilder sb = new StringBuilder("<b>").append(node.title()).append("</b>");
            for (String line : node.lines()) {
                sb.append("<br>").append(line);
            }
            rawLabel = sb.toString();
        }
        // html=1 в draw.io ожидает HTML в атрибуте, но сам XML требует
        // экранирования тегов: <b> -> &lt;b&gt;
        String label = esc(rawLabel.replace("\n", "<br>"));
        String style = switch (node.kind()) {
            case PERSON -> "rounded=0;whiteSpace=wrap;html=1;fillColor=#083D6B;fontColor=#FFFFFF;strokeColor=none;";
            case CONTAINER -> "rounded=1;arcSize=10;whiteSpace=wrap;html=1;fillColor=#1168BD;fontColor=#FFFFFF;strokeColor=none;";
            case DATABASE -> "shape=cylinder3;boundedLbl=1;backgroundOutline=0;size=15;whiteSpace=wrap;html=1;fillColor=#1B6D4A;fontColor=#FFFFFF;strokeColor=none;";
            case EXTERNAL -> "rounded=1;arcSize=10;whiteSpace=wrap;html=1;fillColor=#8C8C8C;fontColor=#FFFFFF;strokeColor=none;";
            case BOUNDARY -> "rounded=0;dashed=1;html=1;fillColor=none;strokeColor=#9AA4AE;verticalAlign=top;align=left;spacingLeft=12;spacingTop=4;fontColor=#52606D;fontStyle=1;";
        };
        return "        <mxCell id=\"" + esc(node.id()) + "\" value=\"" + label + "\" style=\"" + style
                + "\" vertex=\"1\" parent=\"1\">\n"
                + "          <mxGeometry x=\"" + node.x() + "\" y=\"" + node.y() + "\" width=\"" + node.w()
                + "\" height=\"" + node.h() + "\" as=\"geometry\"/>\n"
                + "        </mxCell>\n";
    }

    static String edgeCell(Edge edge) {
        String id = "e-" + edge.from() + "-" + edge.to();
        String style = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;"
                + "strokeColor=" + (edge.jdbc() ? "#5B7FA6" : "#C2681B") + ";"
                + (edge.jdbc() ? "dashed=1;" : "strokeWidth=2;")
                + "fontSize=10;labelBackgroundColor=#FFFFFF;";
        StringBuilder xml = new StringBuilder();
        xml.append("        <mxCell id=\"").append(esc(id)).append("\" value=\"")
                .append(edge.label() == null ? "" : esc(edge.label()))
                .append("\" style=\"").append(style).append("\" edge=\"1\" parent=\"1\" source=\"")
                .append(esc(edge.from())).append("\" target=\"").append(esc(edge.to())).append("\">\n");
        xml.append("          <mxGeometry relative=\"1\" as=\"geometry\">\n");
        if (!edge.via().isEmpty()) {
            xml.append("            <Array as=\"points\">\n");
            for (Point point : edge.via()) {
                xml.append("              <mxPoint x=\"").append(point.x).append("\" y=\"").append(point.y).append("\"/>\n");
            }
            xml.append("            </Array>\n");
        }
        xml.append("          </mxGeometry>\n");
        xml.append("        </mxCell>\n");
        return xml.toString();
    }

    static String textCell(String id, int x, int y, int w, int h, String text, String extraStyle) {
        return "        <mxCell id=\"" + esc(id) + "\" value=\"" + esc(text) + "\" style=\"text;html=1;align=left;verticalAlign=top;"
                + "fillColor=none;strokeColor=none;whiteSpace=wrap;"
                + extraStyle + "\" vertex=\"1\" parent=\"1\">\n"
                + "          <mxGeometry x=\"" + x + "\" y=\"" + y + "\" width=\"" + w + "\" height=\"" + h
                + "\" as=\"geometry\"/>\n"
                + "        </mxCell>\n";
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
