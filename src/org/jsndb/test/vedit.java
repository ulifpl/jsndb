package org.jsndb.test;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class vedit {

	private JFrame frame;
	private JTextField textField;
	private JTable table;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					vedit window = new vedit();
					window.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public vedit() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setFrame(new JFrame());
		getFrame().setBounds(100, 100, 450, 300);
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		getFrame().getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		setTextField(new JTextField());
		panel.add(getTextField(), BorderLayout.NORTH);
		getTextField().setColumns(10);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

		JButton btnIndice = new JButton("indice");
		panel_1.add(btnIndice);

		JScrollPane scrollPane = new JScrollPane();
		getFrame().getContentPane().add(scrollPane, BorderLayout.CENTER);

		setTable(new JTable());
		scrollPane.setViewportView(getTable());
	}

	private JFrame getFrame() {
		return frame;
	}

	private void setFrame(JFrame frame) {
		this.frame = frame;
	}

	private JTextField getTextField() {
		return textField;
	}

	private void setTextField(JTextField textField) {
		this.textField = textField;
	}

	private JTable getTable() {
		return table;
	}

	private void setTable(JTable table) {
		this.table = table;
	}

}
