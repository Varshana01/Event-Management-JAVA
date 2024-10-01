import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList; // Import for ArrayList
import java.util.HashMap;

public class FinanceScreen extends JFrame {
    // Database connection variables
    private Connection connection;

    // Components for budget creation
    private JTextField budgetNameField, allocatedAmountField;
    private JComboBox<String> categoryComboBox, eventComboBox;
    private JButton createBudgetButton;

    // Components for expense tracking
    private JTextField expenseNameField, expenseAmountField;
    private JComboBox<String> expenseCategoryComboBox;
    private JButton addExpenseButton;

    // Components for displaying budget details and tracking
    private JTextArea budgetDetailsArea;
    private JLabel budgetAlertLabel;

    // Data structure to hold budget and expenses
    private HashMap<String, Double> budgetCategories = new HashMap<>();
    private double totalAllocatedBudget = 0;
    private double totalExpenses = 0;

    // Threshold for budget alerts
    private double budgetThreshold = 80.0; // 80% threshold

    public FinanceScreen() {
        setTitle("Finance Screen - Event Planning Management");
        setSize(600, 500);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        connectToDatabase(); // Ensure database connection is established here

        // Panel for Budget Creation
        JPanel budgetPanel = new JPanel(new GridLayout(5, 2));
        budgetPanel.setBorder(BorderFactory.createTitledBorder("Budget Creation"));

        budgetPanel.add(new JLabel("Select Event:"));
        eventComboBox = new JComboBox<>(getEvents());
        budgetPanel.add(eventComboBox);

        budgetPanel.add(new JLabel("Budget Name:"));
        budgetNameField = new JTextField();
        budgetPanel.add(budgetNameField);

        budgetPanel.add(new JLabel("Allocated Amount:"));
        allocatedAmountField = new JTextField();
        budgetPanel.add(allocatedAmountField);

        budgetPanel.add(new JLabel("Category:"));
        String[] categories = {"Venue", "Catering", "Decorations", "Entertainment"};
        categoryComboBox = new JComboBox<>(categories);
        budgetPanel.add(categoryComboBox);

        createBudgetButton = new JButton("Create Budget");
        budgetPanel.add(createBudgetButton);

        // Panel for Expense Recording
        JPanel expensePanel = new JPanel(new GridLayout(5, 2)); // Fixed to 5 rows
        expensePanel.setBorder(BorderFactory.createTitledBorder("Expense Recording"));

        expensePanel.add(new JLabel("Select Event:"));
        JComboBox<String> expenseEventComboBox = new JComboBox<>(getEvents());
        expensePanel.add(expenseEventComboBox);

        expensePanel.add(new JLabel("Expense Name:"));
        expenseNameField = new JTextField();
        expensePanel.add(expenseNameField);

        expensePanel.add(new JLabel("Expense Amount:"));
        expenseAmountField = new JTextField();
        expensePanel.add(expenseAmountField);

        expensePanel.add(new JLabel("Category:"));
        expenseCategoryComboBox = new JComboBox<>(categories);
        expensePanel.add(expenseCategoryComboBox);

        addExpenseButton = new JButton("Add Expense");
        expensePanel.add(addExpenseButton);

        // Panel for budget details and alerts
        JPanel budgetDetailsPanel = new JPanel(new BorderLayout());
        budgetDetailsPanel.setBorder(BorderFactory.createTitledBorder("Budget Details"));

        budgetDetailsArea = new JTextArea();
        budgetDetailsArea.setEditable(false);
        budgetDetailsPanel.add(new JScrollPane(budgetDetailsArea), BorderLayout.CENTER);

        budgetAlertLabel = new JLabel("");
        budgetAlertLabel.setForeground(Color.RED);
        budgetDetailsPanel.add(budgetAlertLabel, BorderLayout.SOUTH);

        // Add panels to the main frame
        add(budgetPanel, BorderLayout.NORTH);
        add(expensePanel, BorderLayout.CENTER);
        add(budgetDetailsPanel, BorderLayout.SOUTH);

        // Event Listeners
        createBudgetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createBudget();
            }
        });

        addExpenseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addExpense(expenseEventComboBox); // Pass the expense event combo box
            }
        });
    }

    private void connectToDatabase() {
        // Adjust your database connection URL, username, and password
        String url = "jdbc:postgresql://localhost:5432/EventsManagementDB"; // Update this line
        String user = "postgres"; // Update this line
        String password = "2801"; // Update this line

        try {
            connection = DriverManager.getConnection(url, user, password);
            if (connection != null) {
                System.out.println("Connected to the database successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
        }
    }

    private String[] getEvents() {
        // This method retrieves existing events from the database
        try {
            String query = "SELECT event_name FROM Event"; // Adjust to your Event table structure
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            ArrayList<String> events = new ArrayList<>();
            while (rs.next()) {
                events.add(rs.getString("event_name")); // Adjust to your column name
            }
            return events.toArray(new String[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private void createBudget() {
        String eventName = (String) eventComboBox.getSelectedItem();
        String budgetName = budgetNameField.getText();
        String category = (String) categoryComboBox.getSelectedItem();
        double allocatedAmount = Double.parseDouble(allocatedAmountField.getText());

        String insertBudgetQuery = "INSERT INTO Budget (event_id, budget_name, category, allocated_amount) " +
                "VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pstmt = connection.prepareStatement(insertBudgetQuery);
            pstmt.setInt(1, getEventId(eventName)); // Method to retrieve event ID
            pstmt.setString(2, budgetName);
            pstmt.setString(3, category);
            pstmt.setDouble(4, allocatedAmount);

            pstmt.executeUpdate();
            budgetCategories.put(category, allocatedAmount);
            totalAllocatedBudget += allocatedAmount;

            budgetDetailsArea.append("Budget Created: " + budgetName + " - Category: " + category + " - Amount: $" + allocatedAmount + "\n");
            checkBudgetThreshold();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addExpense(JComboBox<String> expenseEventComboBox) {
        String eventName = (String) expenseEventComboBox.getSelectedItem(); // Use the passed event combo box
        String expenseName = expenseNameField.getText();
        double expenseAmount = Double.parseDouble(expenseAmountField.getText());
        String category = (String) expenseCategoryComboBox.getSelectedItem();

        // Get budget_id associated with the selected event
        int budgetId = getBudgetId(eventName); // Method to retrieve budget ID for the selected event

        if (budgetId == -1) {
            JOptionPane.showMessageDialog(this, "No budget found for the selected event.");
            return;
        }

        String insertExpenseQuery = "INSERT INTO Expense (budget_id, expense_name, category, amount) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement pstmt = connection.prepareStatement(insertExpenseQuery);
            pstmt.setInt(1, budgetId); // Use the retrieved budget ID
            pstmt.setString(2, expenseName);
            pstmt.setString(3, category);
            pstmt.setDouble(4, expenseAmount);

            pstmt.executeUpdate();
            totalExpenses += expenseAmount;
            budgetDetailsArea.append("Expense Added: " + expenseName + " - Category: " + category + " - Amount: $" + expenseAmount + "\n");

            checkBudgetThreshold();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getBudgetId(String eventName) {
        // Fetch budget ID based on event name (assuming a budget is associated with the event)
        try {
            String query = "SELECT budget_id FROM Budget WHERE event_id = (SELECT event_id FROM Event WHERE event_name = ?)";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, eventName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("budget_id"); // Adjust to your column name
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return an invalid ID if not found
    }

    private int getEventId(String eventName) {
        // Fetch event ID based on event name
        try {
            String query = "SELECT event_id FROM Event WHERE event_name = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, eventName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("event_id"); // Adjust to your column name
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return an invalid ID if not found
    }

    private void checkBudgetThreshold() {
        double budgetUtilization = (totalExpenses / totalAllocatedBudget) * 100;
        if (budgetUtilization > budgetThreshold) {
            budgetAlertLabel.setText("Alert: Budget exceeds " + budgetThreshold + "% threshold!");
        } else {
            budgetAlertLabel.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FinanceScreen().setVisible(true);
        });
    }
}
