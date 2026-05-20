import java.io.*;
import java.util.HashMap;
import java.util.Map;

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
            return;
        }
        System.out.println("\n=============================");
        System.out.println("  Person Identified!");
        System.out.println("  Name       : " + p.name);
        if (p.isStudent()) {
            System.out.println("  USN        : " + valueOrDash(p.usn));
            System.out.println("  Department : Student of " + p.department);
            System.out.println("  Year       : " + valueOrDash(p.year));
        } else {
            System.out.println("  Department : Faculty of " + p.department);
        }
        System.out.println("  College    : MITE College");
        System.out.println("=============================\n");
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
