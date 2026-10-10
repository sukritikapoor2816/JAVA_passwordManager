
import java.awt.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.formdev.flatlaf.FlatDarkLaf;

public class App {

    private static final Color BG = new Color(7, 18, 38);
    private static final Color PANEL = new Color(15, 32, 57);
    private static final Color CARD = new Color(12, 27, 49);
    private static final Color BLUE = new Color(56, 189, 248);
    private static final Color BLUE_HOVER = new Color(125, 211, 252);
    private static final Color TEXT = new Color(241, 245, 249);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color LINE = new Color(37, 62, 91);
    private static final Color HOVER = new Color(24, 48, 77);
    private static final Color GREEN = new Color(52, 211, 153);
    private static final Color RED = new Color(248, 113, 113);

    private static JFrame frame;
    private static CardLayout cards;
    private static JPanel screens;

    private static byte[] activeKey;
    private static String activeEmail;

    private static DefaultTableModel tableModel;
    private static JTable table;
    private static JTextField searchField;
    private static JLabel countLabel;
    private static JLabel accountEmailLabel;
    private static JLabel welcomeLabel;

    private static List<String[]> credentials = new ArrayList<>();
    private static List<Integer> visibleIndexes = new ArrayList<>();

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();

            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ProgressBar.arc", 10);

            UIManager.put("Panel.background", BG);
            UIManager.put("Label.foreground", TEXT);

            UIManager.put("Button.background", PANEL);
            UIManager.put("Button.foreground", TEXT);
            UIManager.put("Button.hoverBackground", HOVER);
            UIManager.put("Button.pressedBackground", new Color(30, 64, 96));

            UIManager.put("TextField.background", CARD);
            UIManager.put("PasswordField.background", CARD);
            UIManager.put("TextField.foreground", TEXT);
            UIManager.put("PasswordField.foreground", TEXT);
            UIManager.put("Component.focusColor", BLUE);
            UIManager.put("Component.borderColor", LINE);

            UIManager.put("Table.background", CARD);
            UIManager.put("Table.foreground", TEXT);
            UIManager.put("Table.selectionBackground", HOVER);
            UIManager.put("Table.selectionForeground", TEXT);
            UIManager.put("TableHeader.background", PANEL);
            UIManager.put("TableHeader.foreground", BLUE);

            UIManager.put("ScrollBar.thumb", new Color(55, 85, 120));
            UIManager.put("ScrollBar.track", BG);
            UIManager.put("OptionPane.background", PANEL);
            UIManager.put("OptionPane.messageForeground", TEXT);

        } catch (Exception ex) {
            System.err.println("FlatLaf could not be loaded; using default theme.");
        }

        SwingUtilities.invokeLater(App::createApp);
    }

    private static void createApp() {
        frame = new JFrame("VAULT  |  Password Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1180, 760);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);

        cards = new CardLayout();
        screens = new JPanel(cards);
        screens.setBackground(BG);

        screens.add(createLoginScreen(), "login");
        screens.add(createRegisterScreen(), "register");
        screens.add(createVaultScreen(), "vault");

        frame.setContentPane(screens);
        cards.show(screens, "login");
        frame.setVisible(true);
    }

    // LOGIN SCREEN

    private static JPanel createLoginScreen() {
        JPanel page = authPage();

        page.add(Box.createVerticalGlue());
        page.add(brand());
        page.add(Box.createVerticalStrut(28));
        page.add(heading("Welcome back"));
        page.add(Box.createVerticalStrut(8));
        page.add(subtitle("Your passwords, protected in one place."));
        page.add(Box.createVerticalStrut(28));

        JTextField email = textField();
        JPasswordField password = passwordField();

        JButton login = primary("Sign In  →");
        JButton goRegister = link("New to Vault?  Create an account");

        JPanel form = formPanel();
        addField(form, "EMAIL ADDRESS", email);
        addField(form, "MASTER PASSWORD", password);

        form.add(Box.createVerticalStrut(5));
        form.add(login);
        form.add(Box.createVerticalStrut(14));
        form.add(goRegister);

        page.add(form);
        page.add(Box.createVerticalGlue());

        JLabel footer = subtitle("🔒  Your vault. Your privacy. Your control.");
        page.add(footer);

        login.addActionListener(e -> {
            String address = email.getText().trim();
            char[] entered = password.getPassword();

            if (address.isEmpty() || entered.length == 0) {
                message("Please enter your email and master password.");
                Arrays.fill(entered, '\0');
                return;
            }

            try {
                if (!AccountManager.login(address, entered)) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Incorrect email or master password.",
                        "Login failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                byte[] derived =
                    AccountManager.deriveEncryptionKey(address, entered);

                activeKey = derived;
                activeEmail = address;

                if (accountEmailLabel != null) {
                    accountEmailLabel.setText(
                        "<html><div style='width:165px'>"
                        + html(activeEmail)
                        + "</div></html>"
                    );
                }

                if (welcomeLabel != null) {
                    welcomeLabel.setText("Welcome back to your vault");
                }

                try {
                    reloadCredentials();
                    cards.show(screens, "vault");
                } catch (Exception ex) {
                    clearSession();
                    showError("Could not open your vault.", ex);
                }

            } catch (Exception ex) {
                showError("Could not verify your account.", ex);
            } finally {
                Arrays.fill(entered, '\0');
                password.setText(" ");
            }
        });

        goRegister.addActionListener(e -> cards.show(screens, "register"));

        return page;
    }

    // REGISTRATION SCREEN

    private static JPanel createRegisterScreen() {
        JPanel page = authPage();

        page.add(Box.createVerticalGlue());
        page.add(brand());
        page.add(Box.createVerticalStrut(22));
        page.add(heading("Create your vault"));
        page.add(Box.createVerticalStrut(8));
        page.add(subtitle("One master password. All your logins."));
        page.add(Box.createVerticalStrut(22));

        JTextField email = textField();
        JPasswordField password = passwordField();
        JPasswordField confirm = passwordField();

        JButton register = primary("Create My Account  →");
        JButton back = link("Already have an account?  Sign in");

        JPanel form = formPanel();

        addField(form, "EMAIL ADDRESS", email);
        addField(form, "MASTER PASSWORD  ·  MINIMUM 12 CHARACTERS", password);
        addField(form, "CONFIRM MASTER PASSWORD", confirm);

        form.add(Box.createVerticalStrut(4));
        form.add(register);
        form.add(Box.createVerticalStrut(12));
        form.add(back);

        page.add(form);
        page.add(Box.createVerticalGlue());

        register.addActionListener(e -> {
            String address = email.getText().trim();
            char[] secret = password.getPassword();
            char[] repeated = confirm.getPassword();

            try {
                if (address.isEmpty() || secret.length == 0
                        || repeated.length == 0) {
                    message("Please fill in all fields.");

                } else if (!address.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    message("Please enter a valid email address.");

                } else if (secret.length < 12) {
                    message("Use a master password with at least 12 characters.");

                } else if (!Arrays.equals(secret, repeated)) {
                    message("The passwords do not match.");

                } else if (AccountManager.register(address, secret)) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Your account has been created successfully.",
                        "Welcome to Vault",
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    email.setText("");
                    password.setText("");
                    confirm.setText("");

                    cards.show(screens, "login");

                } else {
                    message("An account with this email already exists.");
                }

            } catch (Exception ex) {
                showError("Could not create your account.", ex);
            } finally {
                Arrays.fill(secret, '\0');
                Arrays.fill(repeated, '\0');
            }
        });

        back.addActionListener(e -> cards.show(screens, "login"));

        return page;
    }

    // MAIN VAULT DASHBOARD

    private static JPanel createVaultScreen() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // LEFT SIDEBAR

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL);
        sidebar.setBorder(new EmptyBorder(28, 18, 24, 18));
        sidebar.setPreferredSize(new Dimension(235, 0));

        JLabel logo = new JLabel("◆  VAULT");
        logo.setForeground(BLUE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 25));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("PASSWORD MANAGER");
        tagline.setForeground(MUTED);
        tagline.setFont(new Font("SansSerif", Font.BOLD, 9));
        tagline.setBorder(new EmptyBorder(6, 2, 0, 0));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel workspace = new JLabel("WORKSPACE");
        workspace.setForeground(MUTED);
        workspace.setFont(new Font("SansSerif", Font.BOLD, 10));
        workspace.setBorder(new EmptyBorder(38, 10, 12, 0));
        workspace.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton navVault = sidebarButton("▣    My Passwords", true);
        JButton navAdd = sidebarButton("＋    Add Password", false);

        navVault.addActionListener(e -> {
            searchField.setText("");
            updateTable();
        });

        navAdd.addActionListener(e -> addCredential());

        sidebar.add(logo);
        sidebar.add(tagline);
        sidebar.add(workspace);
        sidebar.add(navVault);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(navAdd);
        sidebar.add(Box.createVerticalGlue());

        JSeparator separator = new JSeparator();
        separator.setForeground(LINE);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sidebar.add(separator);
        sidebar.add(Box.createVerticalStrut(20));

        JLabel signedIn = new JLabel("SIGNED IN AS");
        signedIn.setForeground(MUTED);
        signedIn.setFont(new Font("SansSerif", Font.BOLD, 10));
        signedIn.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountEmailLabel = new JLabel("Your account");
        accountEmailLabel.setForeground(TEXT);
        accountEmailLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        accountEmailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logout = sidebarButton("↪    Log Out", false);
        logout.addActionListener(e -> logout());

        sidebar.add(signedIn);
        sidebar.add(Box.createVerticalStrut(7));
        sidebar.add(accountEmailLabel);
        sidebar.add(Box.createVerticalStrut(15));
        sidebar.add(logout);

        // MAIN CONTENT

        JPanel content = new JPanel(new BorderLayout(0, 22));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(30, 32, 28, 32));

        // HEADER

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Passwords");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));

        welcomeLabel = new JLabel("Your secure digital vault");
        welcomeLabel.setForeground(MUTED);
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        titles.add(title);
        titles.add(Box.createVerticalStrut(7));
        titles.add(welcomeLabel);

        JButton addButton = primary("＋  Add Password");
        addButton.setPreferredSize(new Dimension(175, 45));
        addButton.setMaximumSize(new Dimension(175, 45));
        addButton.addActionListener(e -> addCredential());

        header.add(titles, BorderLayout.CENTER);
        header.add(addButton, BorderLayout.EAST);

        // STATISTICS CARD

        JPanel stats = new JPanel(new BorderLayout(16, 0));
        stats.setBackground(CARD);
        stats.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(17, 20, 17, 20)
        ));

        JLabel statIcon = new JLabel("🔐");
        statIcon.setFont(new Font("SansSerif", Font.PLAIN, 29));

        JPanel statText = new JPanel();
        statText.setOpaque(false);
        statText.setLayout(new BoxLayout(statText, BoxLayout.Y_AXIS));

        JLabel statTitle = new JLabel("SAVED CREDENTIALS");
        statTitle.setForeground(MUTED);
        statTitle.setFont(new Font("SansSerif", Font.BOLD, 11));

        countLabel = new JLabel("0 credentials");
        countLabel.setForeground(BLUE);
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 25));

        statText.add(statTitle);
        statText.add(Box.createVerticalStrut(6));
        statText.add(countLabel);

        stats.add(statIcon, BorderLayout.WEST);
        stats.add(statText, BorderLayout.CENTER);
        stats.setPreferredSize(new Dimension(400, 95));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        // SEARCH BAR

        searchField = textField();
        searchField.setPreferredSize(new Dimension(300, 44));
        searchField.addActionListener(e -> updateTable());

        JButton search = secondary("⌕  Search");
        search.setPreferredSize(new Dimension(115, 44));
        search.addActionListener(e -> updateTable());

        JButton clearSearch = secondary("Clear");
        clearSearch.setPreferredSize(new Dimension(80, 44));
        clearSearch.addActionListener(e -> {
            searchField.setText("");
            updateTable();
        });

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchActions.setOpaque(false);
        searchActions.add(search);
        searchActions.add(clearSearch);

        searchPanel.add(searchActions, BorderLayout.EAST);

        // CREDENTIALS TABLE

        tableModel = new DefaultTableModel(
            new Object[]{"WEBSITE / SERVICE", "USERNAME / EMAIL", "PASSWORD"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(43);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(CARD);
        table.setSelectionBackground(HOVER);
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setBackground(PANEL);
        table.getTableHeader().setForeground(BLUE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(CARD);
        cellRenderer.setForeground(TEXT);
        cellRenderer.setBorder(new EmptyBorder(0, 12, 0, 8));

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(CARD);

        // TABLE CARD

        JButton view = secondary("◉  View");
        JButton delete = danger("✕  Delete");

        view.addActionListener(e -> viewCredential());
        delete.addActionListener(e -> deleteCredential());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(view);
        actions.add(delete);

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("Your saved logins");
        tableTitle.setForeground(TEXT);
        tableTitle.setFont(new Font("SansSerif", Font.BOLD, 17));

        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(actions, BorderLayout.EAST);

        JLabel hint = new JLabel(
            "  Select a login to view its password or delete the entry."
        );
        hint.setForeground(MUTED);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel tableCard = new JPanel(new BorderLayout(0, 14));
        tableCard.setBackground(CARD);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(17, 17, 14, 17)
        ));

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(hint, BorderLayout.SOUTH);

        // DASHBOARD LAYOUT

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(stats);
        center.add(Box.createVerticalStrut(18));
        center.add(searchPanel);
        center.add(Box.createVerticalStrut(18));
        center.add(tableCard);

        content.add(header, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);

        return root;
    }

    // LOAD AND DISPLAY CREDENTIALS

    private static void reloadCredentials() throws Exception {
        credentials = CredentialStore.getAll(activeEmail, activeKey);
        updateTable();
    }

    private static void updateTable() {
        if (tableModel == null || searchField == null) {
            return;
        }

        tableModel.setRowCount(0);
        visibleIndexes.clear();

        String query = searchField.getText().trim()
            .toLowerCase(java.util.Locale.ROOT);

        for (int i = 0; i < credentials.size(); i++) {
            String[] c = credentials.get(i);

            if (c[0].toLowerCase(java.util.Locale.ROOT).contains(query)
                    || c[1].toLowerCase(java.util.Locale.ROOT).contains(query)) {

                tableModel.addRow(new Object[]{
                    c[0],
                    c[1],
                    "••••••••"
                });

                visibleIndexes.add(i);
            }
        }

        countLabel.setText(
            credentials.size()
            + (credentials.size() == 1 ? " credential" : " credentials")
        );
    }

    private static int selectedCredentialIndex() {
        int row = table.getSelectedRow();

        if (row < 0) {
            message("Please select a credential first.");
            return -1;
        }

        int modelRow = table.convertRowIndexToModel(row);

        if (modelRow < 0 || modelRow >= visibleIndexes.size()) {
            return -1;
        }

        return visibleIndexes.get(modelRow);
    }

    // ADD CREDENTIAL

    private static void addCredential() {
        if (activeKey == null) {
            return;
        }

        JTextField website = textField();
        JTextField username = textField();
        JPasswordField password = passwordField();

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(PANEL);
        form.setBorder(new EmptyBorder(16, 18, 10, 18));
        form.setPreferredSize(new Dimension(390, 245));

        addField(form, "WEBSITE OR SERVICE", website);
        addField(form, "USERNAME OR EMAIL", username);
        addField(form, "PASSWORD", password);

        int result = JOptionPane.showConfirmDialog(
            frame,
            form,
            "Add a Password",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        char[] secret = password.getPassword();

        try {
            String site = website.getText().trim();
            String user = username.getText().trim();

            if (site.isEmpty() || user.isEmpty() || secret.length == 0) {
                message("Please fill in all three fields.");
                return;
            }

            CredentialStore.save(
                activeEmail,
                site,
                user,
                new String(secret),
                activeKey
            );

            reloadCredentials();

            JOptionPane.showMessageDialog(
                frame,
                "Your password has been saved securely.",
                "Password Saved",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            showError("Could not save the password.", ex);
        } finally {
            Arrays.fill(secret, '\0');
            password.setText("");
        }
    }

    // VIEW CREDENTIAL

    private static void viewCredential() {
        int index = selectedCredentialIndex();

        if (index < 0) {
            return;
        }

        String[] c = credentials.get(index);

        JPasswordField secret = passwordField();
        secret.setText(c[2]);
        secret.setEditable(false);

        JCheckBox reveal = new JCheckBox("Show password");
        reveal.setOpaque(false);
        reveal.setForeground(TEXT);
        reveal.setFocusPainted(false);

        char echo = secret.getEchoChar();

        reveal.addActionListener(e -> {
            secret.setEchoChar(reveal.isSelected() ? (char) 0 : echo);
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(18, 18, 12, 18));
        panel.setPreferredSize(new Dimension(390, 230));

        JLabel siteTitle = dialogLabel("WEBSITE / SERVICE");
        JLabel siteValue = dialogValue(c[0]);

        JLabel userTitle = dialogLabel("USERNAME / EMAIL");
        JLabel userValue = dialogValue(c[1]);

        JLabel passTitle = dialogLabel("PASSWORD");

        panel.add(siteTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(siteValue);
        panel.add(Box.createVerticalStrut(15));

        panel.add(userTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(userValue);
        panel.add(Box.createVerticalStrut(15));

        panel.add(passTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(secret);
        panel.add(Box.createVerticalStrut(8));
        panel.add(reveal);

        JOptionPane.showMessageDialog(
            frame,
            panel,
            "Credential Details",
            JOptionPane.INFORMATION_MESSAGE
        );

        secret.setText("");
    }

    // DELETE CREDENTIAL

    private static void deleteCredential() {
        int index = selectedCredentialIndex();

        if (index < 0) {
            return;
        }

        String[] c = credentials.get(index);

        int confirm = JOptionPane.showConfirmDialog(
            frame,
            "Delete the saved login for " + c[0] + " (" + c[1] + ")?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            CredentialStore.delete(activeEmail, index, activeKey);
            reloadCredentials();

            JOptionPane.showMessageDialog(
                frame,
                "The saved login has been deleted.",
                "Credential Deleted",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            showError("Could not delete the credential.", ex);
        }
    }

    // LOGOUT AND SESSION CLEANUP

    private static void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            frame,
            "Are you sure you want to log out of your vault?",
            "Log Out",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        clearSession();

        if (searchField != null) {
            searchField.setText("");
        }

        if (tableModel != null) {
            tableModel.setRowCount(0);
        }

        if (countLabel != null) {
            countLabel.setText("0 credentials");
        }

        if (accountEmailLabel != null) {
            accountEmailLabel.setText("Your account");
        }

        cards.show(screens, "login");
    }

    private static void clearSession() {
        if (activeKey != null) {
            Arrays.fill(activeKey, (byte) 0);
            activeKey = null;
        }

        activeEmail = null;
        credentials = new ArrayList<>();
        visibleIndexes.clear();
    }

    // AUTHENTICATION LAYOUT HELPERS

    private static JPanel authPage() {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(25, 20, 25, 20));
        return page;
    }

    private static JPanel formPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL);

        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(28, 30, 25, 30)
        ));

        panel.setMaximumSize(new Dimension(450, 530));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        return panel;
    }

    private static JLabel brand() {
        JLabel label = new JLabel("◆  VAULT");
        label.setFont(new Font("SansSerif", Font.BOLD, 23));
        label.setForeground(BLUE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 29));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // INPUT FIELDS

    private static JTextField textField() {
        JTextField field = new JTextField();

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(CARD);
        field.setCaretColor(BLUE);
        field.setSelectionColor(HOVER);
        field.setSelectedTextColor(TEXT);

        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(10, 12, 10, 12)
        ));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setPreferredSize(new Dimension(380, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        return field;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(CARD);
        field.setCaretColor(BLUE);
        field.setSelectionColor(HOVER);
        field.setSelectedTextColor(TEXT);

        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(10, 12, 10, 12)
        ));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setPreferredSize(new Dimension(380, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        return field;
    }

    private static void addField(
            JPanel form, String title, JTextField field) {

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(label);
        form.add(Box.createVerticalStrut(8));
        form.add(field);
        form.add(Box.createVerticalStrut(17));
    }

    // BUTTON STYLES

    private static JButton primary(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(new Color(7, 18, 38));
        button.setBackground(BLUE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(390, 46));
        button.setPreferredSize(new Dimension(165, 44));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(BLUE_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(BLUE);
            }
        });

        return button;
    }

    private static JButton secondary(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(BLUE);
        button.setBackground(HOVER);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(8, 14, 8, 14)
        ));

        return button;
    }

    private static JButton danger(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(RED);
        button.setBackground(new Color(65, 28, 45));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(110, 48, 65)),
            new EmptyBorder(8, 14, 8, 14)
        ));

        return button;
    }

    private static JButton link(String text) {
        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setForeground(BLUE);
        button.setBackground(PANEL);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        return button;
    }

    private static JButton sidebarButton(String text, boolean selected) {
        JButton button = new JButton(text);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(selected ? BLUE : TEXT);
        button.setBackground(selected ? HOVER : PANEL);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 12, 12, 12));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        return button;
    }

    // DIALOG LABEL HELPERS

    private static JLabel dialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(MUTED);
        return label;
    }

    private static JLabel dialogValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(TEXT);
        return label;
    }

    // GENERAL HELPERS

    private static String html(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static void message(String text) {
        JOptionPane.showMessageDialog(
            frame,
            text,
            "Vault",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
            frame,
            message + "\n" + ex.getMessage(),
            "Vault Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
