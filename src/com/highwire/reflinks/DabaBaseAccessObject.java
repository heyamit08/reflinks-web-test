package com.highwire.reflinks;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;

/**
 * @author amits
 * 
 */
public class DabaBaseAccessObject {

	private static BasicDataSource basicDataSource;

	public static Connection getConnection()
			throws SQLException {

		Connection conn = null;
		try {

			if (basicDataSource == null) {
				basicDataSource = new BasicDataSource();
				basicDataSource.setDriverClassName("com.sybase.jdbc2.jdbc.SybDriver");
				basicDataSource
						.setUrl("jdbc:sybase:Tds:171.66.124.77:1025/reflinksdev");
				basicDataSource.setUsername("journalmaint");
				basicDataSource.setPassword("r00frat");
			}
			conn = basicDataSource.getConnection();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;

	}
}
