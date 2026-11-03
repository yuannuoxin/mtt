import java.sql.*;
import java.io.*;

public class QueryToFile {
    public static void main(String[] args) throws Exception {
        String sql = args[0];
        String out = args[1];
        StringBuilder sb = new StringBuilder();
        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/mydb", "postgres", "703b1a0b4fa74657ba37d7c548941d82");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) sb.append(md.getColumnLabel(i)).append("\t");
            sb.append("\n");
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) sb.append(rs.getString(i) == null ? "null" : rs.getString(i)).append("\t");
                sb.append("\n");
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8")) {
            w.write(sb.toString());
        }
    }
}
