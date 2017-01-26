package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Andrew G. West - unicode_test.java - Trying to investigate some funny
 * things going on with a "circumflex" character and how it is handled
 * both in Java, MySQL, and the JDBC between them. 
 */
public class unicode_test{


	// ALTER TABLE users_explicit convert to character set utf8; 
	
	// SELECT character_set_name FROM information_schema.`COLUMNS` 
	// WHERE table_schema = "presta_stiki"
	// AND table_name = "users_explicit"
	// AND column_name = "USER";
	
	/**
	 * Test harness.
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{

			// *** REMEMBER TO BLANK THESE BEFORE GITHUB ***
		String url = "****"; 
		String user = "****";
		String pass = "****";
		Connection con = null; // Then proceed to connect
		try{ // In the case of an error, just return NULL
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection(url, user, pass);
		} catch(Exception e){
			System.err.println("Error opening DB connection");
			e.printStackTrace();
		} // Also output a message to system.err

		String unicode = "Psiĥedelisto";
		String sql_insert = "INSERT INTO users_explicit VALUES(?);";
		PreparedStatement pstmt_insert = con.prepareStatement(sql_insert);
		pstmt_insert.setString(1, unicode);
		pstmt_insert.executeUpdate();
		
		String sql_query = "SELECT COUNT(*) FROM users_explicit WHERE USER=?;";
		PreparedStatement pstmt_query = con.prepareStatement(sql_query);
		pstmt_query.setString(1, unicode);
		ResultSet rs = pstmt_query.executeQuery();
		rs.next();
		System.out.println(rs.getInt(1));
		
		pstmt_insert.close();
		pstmt_query.close();
		con.close();
	}
	
}
