import java.sql.*;

public class CheckFp {
    public static void main(String[] args) throws Exception {
        String sql = args[0];
        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/mydb", "postgres", "703b1a0b4fa74657ba37d7c548941d82");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= cols; i++) header.append(md.getColumnLabel(i)).append("\t");
            System.out.println(header.toString().trim());
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) row.append(rs.getString(i) == null ? "null" : rs.getString(i)).append("\t");
                System.out.println(row.toString().trim());
            }
        }
    }
}
