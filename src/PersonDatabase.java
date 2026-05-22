import java.io.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class PersonDatabase {

    // In-memory cache: imageFile -> Person
    private static final Map<String, Person> cache = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Load all records from persons.csv into memory.
     * Call once at startup from MainApp.
     *
     * CSV format (with header row):
     *   id,type,name,usn,department,year,imageFile
     *
     * Example:
     *   1,student,Rahul Sharma,4MT22IS001,ISE,2nd Year,person1_1.jpg
     *   2,faculty,Dr Anitha,,ISE,,person2_1.jpg
     */
    public static void loadDatabase() {

        cache.clear();

        try (BufferedReader br = new BufferedReader(
                new FileReader(ProjectPaths.resolve("database/persons.csv")))) {

            String header = br.readLine(); // skip header row
            if (header == null) {
                System.err.println("WARNING: persons.csv is empty.");
                return;
            }

            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {

                line = line.trim();
                if (line.isEmpty()) continue;

                String[] data = line.split(",", -1);

                if (data.length < 6) {
                    System.out.println("Skipping malformed row: " + line);
                    continue;
                }

                Person p;
                if (data.length >= 7) {
                    p = new Person(
                            data[0].trim(),
                            data[1].trim(),
                            data[2].trim(),
                            data[3].trim(),
                            data[4].trim(),
                            data[5].trim(),
                            data[6].trim()
                    );
                } else {
                    // Backward compatibility for the old CSV:
                    // id,name,age,department,email,imageFile
                    p = new Person(
                            data[0].trim(),
                            "student",
                            data[1].trim(),
                            "",
                            data[3].trim(),
                            data[2].trim(),
                            data[5].trim()
                    );
                }

                cache.put(p.imageFile, p);
                System.out.println("Loaded person: " + p.name + " -> " + p.imageFile);
                count++;
            }

            System.out.println("Database loaded: " + count + " person(s).");
            loaded = true;

        } catch (FileNotFoundException e) {
            System.err.println("ERROR: database/persons.csv not found.");
            System.err.println("Make sure the file exists at: "
                    + ProjectPaths.resolve("database/persons.csv").getAbsolutePath());
        } catch (IOException e) {
            System.err.println("ERROR reading persons.csv:");
            e.printStackTrace();
        }
    }

    /**
     * Returns the person's name for a given imageFile key, or "Unknown".
     */
    public static String getPersonName(String imageFile) {
        ensureLoaded();
        Person p = findPerson(imageFile);
        if (p == null) {
            System.out.println("No match in DB for key: " + imageFile);
            return "Unknown";
        }
        return p.name;
    }

    /**
     * Prints full details for the person associated with imageFile.
     */
    public static void showPersonDetails(String imageFile) {
        ensureLoaded();
        Person p = findPerson(imageFile);
        if (p == null) {
            System.out.println("No database entry found for: " + imageFile);
            showResultDialog(
                    "No Record Found",
                    new String[][] {
                            {"Image", imageFile},
                            {"Status", "No database entry found"}
                    },
                    false);
            return;
        }

        String[][] details;
        if (p.isStudent()) {
            details = new String[][] {
                    {"Name", p.name},
                    {"USN", valueOrDash(p.usn)},
                    {"Department", "Student of " + valueOrDash(p.department)},
                    {"Year", valueOrDash(p.year)},
                    {"College", "MITE College"}
            };
        } else {
            details = new String[][] {
                    {"Name", p.name},
                    {"Department", "Faculty of " + valueOrDash(p.department)},
                    {"College", "MITE College"}
            };
        }

        String message;
        if (p.isStudent()) {
            message = String.format(
                    "Person Identified!%n%nName       : %s%nUSN        : %s%nDepartment : Student of %s%nYear       : %s%nCollege    : MITE College",
                    p.name,
                    valueOrDash(p.usn),
                    valueOrDash(p.department),
                    valueOrDash(p.year));
        } else {
            message = String.format(
                    "Person Identified!%n%nName       : %s%nDepartment : Faculty of %s%nCollege    : MITE College",
                    p.name,
                    valueOrDash(p.department));
        }

        System.out.println("\n=============================");
        System.out.println("  " + message.replace(System.lineSeparator(), System.lineSeparator() + "  "));
        System.out.println("=============================\n");

        showResultDialog("Person Identified", details, true);
    }

    /**
     * Prints a clear result when the detected face is not registered.
     */
    public static void showUnknownPersonMessage() {
        String message = "Unknown Person Detected!\n\n"
                + "Status     : Not from MITE College\n"
                + "Message    : Person is not registered in the college database.";

        System.out.println("\n=============================");
        System.out.println("  Unknown Person Detected!");
        System.out.println("  Status     : Not from MITE College");
        System.out.println("  Message    : Person is not registered in the college database.");
        System.out.println("=============================\n");

        showResultDialog(
                "Unknown Person Detected",
                new String[][] {
                        {"Status", "Not from MITE College"},
                        {"Message", "Person is not registered in the college database"}
                },
                false);
    }

    private static void showResultDialog(String title, String[][] rows, boolean success) {
        Color accent = success ? new Color(22, 163, 74) : new Color(220, 38, 38);
        Color background = new Color(248, 250, 252);
        Color text = new Color(15, 23, 42);
        Color muted = new Color(71, 85, 105);

        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        panel.setPreferredSize(new Dimension(430, Math.max(250, 150 + rows.length * 34)));

        JLabel header = new JLabel(title, success ? UIManager.getIcon("OptionPane.informationIcon")
                : UIManager.getIcon("OptionPane.warningIcon"), SwingConstants.LEFT);
        header.setIconTextGap(12);
        header.setForeground(accent);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(header, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, accent),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 0, 6, 14);
        gbc.anchor = GridBagConstraints.WEST;

        for (String[] row : rows) {
            JLabel label = new JLabel(row[0] + ":");
            label.setForeground(muted);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));

            JLabel value = new JLabel("<html>" + escapeHtml(row[1]) + "</html>");
            value.setForeground(text);
            value.setFont(value.getFont().deriveFont(Font.PLAIN, 14f));

            gbc.gridx = 0;
            gbc.weightx = 0;
            details.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            details.add(value, gbc);

            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
        }

        panel.add(details, BorderLayout.CENTER);

        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION);
        JDialog dialog = pane.createDialog((Component) null, "Face Recognition Result");
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void ensureLoaded() {
        if (!loaded) loadDatabase();
    }

    private static Person findPerson(String imageFile) {
        Person exact = cache.get(imageFile);
        if (exact != null) {
            return exact;
        }

        String personKey = personKeyFromImage(imageFile);
        for (Person person : cache.values()) {
            if (personKeyFromImage(person.imageFile).equals(personKey)) {
                return person;
            }
        }

        return null;
    }

    private static String personKeyFromImage(String imageFile) {
        String name = imageFile == null ? "" : imageFile.trim();
        int underscore = name.indexOf('_');
        if (underscore > 0) {
            return name.substring(0, underscore);
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    // Run this class directly to test if persons.csv loads correctly
    public static void main(String[] args) {
        loadDatabase();
        System.out.println("\nKeys in cache: " + cache.keySet());
    }

    private static class Person {
        String id, type, name, usn, department, year, imageFile;

        Person(String id, String type, String name, String usn, String department,
               String year, String imageFile) {
            this.id         = id;
            this.type       = type;
            this.name       = name;
            this.usn        = usn;
            this.department = department;
            this.year       = year;
            this.imageFile  = imageFile;
        }

        boolean isStudent() {
            String normalizedType = type == null ? "" : type.trim().toLowerCase();
            return !normalizedType.equals("faculty") && !normalizedType.equals("faulty");
        }
    }
}
