import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * ShopEasy - E-Commerce desktop application built ONLY with Java AWT + Swing.
 *
 * WHERE AWT IS USED
 *   - LoginFrame: built entirely with AWT (Frame, Panel, Label, TextField, Button, Dialog)
 *   - Layout managers: BorderLayout, FlowLayout, GridLayout (java.awt)
 *   - Graphics2D custom drawing of product images (GradientPaint, fillOval, drawString)
 *   - Event handling: ActionListener, MouseListener, WindowAdapter (java.awt.event)
 *   - Color, Font, FontMetrics, Dimension, Insets, EventQueue
 *
 * WHERE SWING IS USED
 *   - MainFrame: JFrame, JMenuBar, JTabbedPane, JTable, JList, JSplitPane,
 *     JScrollPane, JSpinner-free buttons, JComboBox, JTextArea, JFileChooser, JOptionPane
 *
 * Compile: javac ECommerceApp.java
 * Run:     java ECommerceApp
 * Demo login: admin / admin   (admin gets an extra "Admin" tab)
 */
public class ECommerceApp {

    // =====================================================================
    //  DATA MODEL (in-memory)
    // =====================================================================
    static class Product {
        int id; String name; String category; double price; Color color;
        Product(int id, String name, String category, double price) {
            this.id = id; this.name = name; this.category = category; this.price = price;
            float hue = ((name.hashCode() & 0x7fffffff) % 360) / 360f;
            this.color = Color.getHSBColor(hue, 0.55f, 0.85f);
        }
    }

    static class Order {
        int id; String user; String text; double total;
        Order(int id, String user, String text, double total) {
            this.id = id; this.user = user; this.text = text; this.total = total;
        }
        public String toString() { return String.format("Order #%d  -  Rs. %.2f", id, total); }
    }

    static final List<Product> products = new ArrayList<>();
    static final Map<String, String> users = new HashMap<>();
    static final Map<Integer, Integer> cart = new LinkedHashMap<>();   // productId -> qty
    static final List<Order> orders = new ArrayList<>();
    static int nextProductId = 1, nextOrderId = 1001;
    static String currentUser;

    static final Color PRIMARY = new Color(33, 97, 140);
    static final Color ACCENT = new Color(39, 174, 96);
    static final Color DANGER = new Color(192, 57, 43);
    static final Color BG = new Color(236, 240, 241);

    static void addProduct(String name, String cat, double price) {
        products.add(new Product(nextProductId++, name, cat, price));
    }

    static Product findProduct(int id) {
        for (Product p : products) if (p.id == id) return p;
        return null;
    }

    static void seed() {
        users.put("admin", "admin");
        addProduct("Wireless Mouse", "Electronics", 599);
        addProduct("Bluetooth Headphones", "Electronics", 1999);
        addProduct("USB-C Charger", "Electronics", 799);
        addProduct("Smart Watch", "Electronics", 3499);
        addProduct("Cotton T-Shirt", "Clothing", 499);
        addProduct("Denim Jeans", "Clothing", 1299);
        addProduct("Running Shoes", "Clothing", 2499);
        addProduct("Hoodie", "Clothing", 1499);
        addProduct("Java Programming Book", "Books", 650);
        addProduct("Data Structures Book", "Books", 720);
        addProduct("Notebook Pack", "Books", 250);
        addProduct("Water Bottle", "Home", 349);
        addProduct("Desk Lamp", "Home", 899);
        addProduct("Backpack", "Home", 1199);
    }

    // ---------- small UI helpers ----------
    static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        return b;
    }

    // =====================================================================
    //  LOGIN SCREEN  -  pure AWT (Frame, Panel, Label, TextField, Button, Dialog)
    // =====================================================================
    static class LoginFrame extends Frame {
        private final TextField userF = new TextField(16);
        private final TextField passF = new TextField(16);
        private final Label msg = new Label("", Label.CENTER);

        LoginFrame() {
            super("ShopEasy - Login (AWT)");
            setLayout(new BorderLayout(10, 10));
            setBackground(BG);

            Label title = new Label("ShopEasy", Label.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(PRIMARY);
            add(title, BorderLayout.NORTH);

            Panel form = new Panel(new GridLayout(2, 2, 10, 14));
            form.add(new Label("Username:", Label.RIGHT));
            form.add(userF);
            form.add(new Label("Password:", Label.RIGHT));
            passF.setEchoChar('*');
            form.add(passF);
            Panel formWrap = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 20));
            formWrap.add(form);
            add(formWrap, BorderLayout.CENTER);

            Panel south = new Panel(new GridLayout(2, 1, 5, 5));
            Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 5));
            Button login = new Button("Login");
            Button register = new Button("Register");
            Button exit = new Button("Exit");
            buttons.add(login); buttons.add(register); buttons.add(exit);
            south.add(buttons);
            msg.setForeground(DANGER);
            south.add(msg);
            add(south, BorderLayout.SOUTH);

            login.addActionListener(e -> doLogin());
            passF.addActionListener(e -> doLogin());
            register.addActionListener(e -> doRegister());
            exit.addActionListener(e -> confirmExit());
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) { confirmExit(); }
            });

            setSize(440, 340);
            setLocationRelativeTo(null);
        }

        private void doLogin() {
            String u = userF.getText().trim(), p = passF.getText();
            if (users.containsKey(u) && users.get(u).equals(p)) {
                currentUser = u;
                dispose();
                new MainFrame().setVisible(true);
            } else {
                msg.setForeground(DANGER);
                msg.setText("Invalid username or password");
            }
        }

        private void doRegister() {
            String u = userF.getText().trim(), p = passF.getText();
            msg.setForeground(DANGER);
            if (u.isEmpty() || p.length() < 4) msg.setText("Username required, password min 4 chars");
            else if (users.containsKey(u)) msg.setText("Username already exists");
            else {
                users.put(u, p);
                msg.setForeground(ACCENT);
                msg.setText("Registered! Now click Login");
            }
        }

        // AWT modal Dialog
        private void confirmExit() {
            final Dialog d = new Dialog(this, "Exit", true);
            d.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
            d.add(new Label("Do you really want to exit ShopEasy?"));
            Button yes = new Button("Yes"), no = new Button("No");
            yes.addActionListener(e -> System.exit(0));
            no.addActionListener(e -> d.dispose());
            d.add(yes); d.add(no);
            d.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) { d.dispose(); }
            });
            d.setSize(300, 120);
            d.setLocationRelativeTo(this);
            d.setVisible(true);
        }
    }

    // =====================================================================
    //  MAIN WINDOW  -  Swing (JFrame, JTabbedPane, JTable, JList, JSplitPane ...)
    // =====================================================================
    static class MainFrame extends JFrame {
        private final JTabbedPane tabs = new JTabbedPane();

        // shop tab
        private final JPanel gridPanel = new JPanel(new GridLayout(0, 3, 12, 12));
        private final JTextField searchF = new JTextField(16);
        private final JComboBox<String> catBox = new JComboBox<>();
        private boolean updatingCats = false;

        // cart tab
        private final DefaultTableModel cartModel = readOnlyModel("ID", "Product", "Price", "Qty", "Subtotal");
        private final JTable cartTable = new JTable(cartModel);
        private final JLabel totalLbl = new JLabel("Total: Rs. 0.00");

        // orders tab
        private final DefaultListModel<Order> orderListModel = new DefaultListModel<>();
        private final JList<Order> orderList = new JList<>(orderListModel);
        private final JTextArea orderDetail = new JTextArea();

        // admin tab
        private final DefaultTableModel adminModel = readOnlyModel("ID", "Name", "Category", "Price");
        private final JTable adminTable = new JTable(adminModel);

        private final JLabel status = new JLabel(" ");

        MainFrame() {
            super("ShopEasy - Logged in as " + currentUser);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(980, 660);
            setLocationRelativeTo(null);
            setJMenuBar(buildMenu());

            tabs.addTab("Shop", buildShopTab());
            tabs.addTab("Cart", buildCartTab());
            tabs.addTab("My Orders", buildOrdersTab());
            if ("admin".equals(currentUser)) tabs.addTab("Admin", buildAdminTab());
            tabs.setFont(new Font("SansSerif", Font.BOLD, 14));

            status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            add(tabs, BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);

            refreshCategories();
            refreshShop();
            refreshCart();
            refreshOrders();
            refreshAdmin();
            status.setText("Welcome, " + currentUser + "! Browse products and click 'Add to Cart'.");
        }

        // ---------- menu bar ----------
        private JMenuBar buildMenu() {
            JMenuBar mb = new JMenuBar();

            JMenu file = new JMenu("File");
            JMenuItem logout = new JMenuItem("Logout");
            logout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
            logout.addActionListener(e -> {
                cart.clear();
                currentUser = null;
                dispose();
                new LoginFrame().setVisible(true);
            });
            JMenuItem exit = new JMenuItem("Exit");
            exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
            exit.addActionListener(e -> System.exit(0));
            file.add(logout); file.addSeparator(); file.add(exit);

            JMenu view = new JMenu("View");
            String[] names = {"Shop", "Cart", "My Orders"};
            for (int i = 0; i < names.length; i++) {
                final int idx = i;
                JMenuItem it = new JMenuItem("Go to " + names[i]);
                it.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK));
                it.addActionListener(e -> tabs.setSelectedIndex(idx));
                view.add(it);
            }

            JMenu help = new JMenu("Help");
            JMenuItem about = new JMenuItem("About");
            about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "ShopEasy v1.0\nE-Commerce app built with Java AWT and Swing\nCollege Project",
                    "About", JOptionPane.INFORMATION_MESSAGE));
            help.add(about);

            mb.add(file); mb.add(view); mb.add(help);
            return mb;
        }

        // ---------- shop tab ----------
        private JPanel buildShopTab() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            bar.add(new JLabel("Search:"));
            bar.add(searchF);
            bar.add(new JLabel("Category:"));
            bar.add(catBox);
            JButton go = btn("Search", PRIMARY);
            JButton clear = btn("Clear", Color.GRAY);
            bar.add(go); bar.add(clear);
            panel.add(bar, BorderLayout.NORTH);

            JPanel holder = new JPanel(new BorderLayout());
            holder.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            holder.add(gridPanel, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(holder);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            panel.add(sp, BorderLayout.CENTER);

            go.addActionListener(e -> refreshShop());
            searchF.addActionListener(e -> refreshShop());
            catBox.addActionListener(e -> { if (!updatingCats) refreshShop(); });
            clear.addActionListener(e -> {
                searchF.setText("");
                catBox.setSelectedIndex(0);
                refreshShop();
            });
            return panel;
        }

        private void refreshCategories() {
            Set<String> cats = new LinkedHashSet<>();
            cats.add("All");
            for (Product p : products) cats.add(p.category);
            Object selected = catBox.getSelectedItem();
            updatingCats = true;
            catBox.setModel(new DefaultComboBoxModel<>(cats.toArray(new String[0])));
            if (selected != null && cats.contains(selected)) catBox.setSelectedItem(selected);
            updatingCats = false;
        }

        private void refreshShop() {
            gridPanel.removeAll();
            String q = searchF.getText().trim().toLowerCase();
            String cat = (String) catBox.getSelectedItem();
            int n = 0;
            for (Product p : products) {
                if (cat != null && !cat.equals("All") && !p.category.equals(cat)) continue;
                if (!p.name.toLowerCase().contains(q)) continue;
                gridPanel.add(new ProductCard(p));
                n++;
            }
            while (n < 3 || n % 3 != 0) {          // keep a 3-column look
                JPanel filler = new JPanel();
                filler.setOpaque(false);
                gridPanel.add(filler);
                n++;
            }
            gridPanel.revalidate();
            gridPanel.repaint();
        }

        /** One product tile: custom AWT Graphics2D art + Swing labels/button. */
        private class ProductCard extends JPanel {
            ProductCard(final Product p) {
                super(new BorderLayout(0, 4));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(189, 195, 199)),
                        BorderFactory.createEmptyBorder(6, 6, 6, 6)));
                setBackground(Color.WHITE);

                ArtPanel art = new ArtPanel(p);
                add(art, BorderLayout.CENTER);

                JPanel info = new JPanel(new GridLayout(3, 1, 0, 3));
                info.setOpaque(false);
                JLabel name = new JLabel(p.name, SwingConstants.CENTER);
                name.setFont(new Font("SansSerif", Font.BOLD, 14));
                JLabel price = new JLabel(String.format("Rs. %.2f   |   %s", p.price, p.category), SwingConstants.CENTER);
                price.setForeground(Color.DARK_GRAY);
                JButton add = btn("Add to Cart", ACCENT);
                add.addActionListener(e -> addToCart(p));
                info.add(name); info.add(price); info.add(add);
                add(info, BorderLayout.SOUTH);
            }
        }

        /** Custom-painted product picture with hover effect (AWT MouseListener + Graphics2D). */
        private static class ArtPanel extends JPanel {
            private final Product p;
            private boolean hover = false;

            ArtPanel(Product p) {
                this.p = p;
                setPreferredSize(new Dimension(200, 130));
                setOpaque(false);
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color c = hover ? p.color.brighter() : p.color;
                g2.setPaint(new GradientPaint(0, 0, c, w, h, c.darker()));
                g2.fillRoundRect(0, 0, w, h, 24, 24);

                int d = Math.min(w, h) - 36;
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillOval((w - d) / 2, (h - d) / 2, d, d);

                g2.setColor(p.color.darker().darker());
                g2.setFont(new Font("SansSerif", Font.BOLD, d / 2));
                FontMetrics fm = g2.getFontMetrics();
                String s = p.name.substring(0, 1).toUpperCase();
                g2.drawString(s, (w - fm.stringWidth(s)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        }

        // ---------- cart tab ----------
        private JPanel buildCartTab() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel title = new JLabel("Your Shopping Cart");
            title.setFont(new Font("SansSerif", Font.BOLD, 22));
            title.setForeground(PRIMARY);
            panel.add(title, BorderLayout.NORTH);

            cartTable.setRowHeight(28);
            cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
            panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            totalLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
            bottom.add(totalLbl, BorderLayout.WEST);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton plus = btn("+ Qty", PRIMARY), minus = btn("- Qty", PRIMARY);
            JButton remove = btn("Remove", DANGER), clearBtn = btn("Clear Cart", Color.GRAY);
            JButton checkout = btn("Checkout", ACCENT);
            btns.add(plus); btns.add(minus); btns.add(remove); btns.add(clearBtn); btns.add(checkout);
            bottom.add(btns, BorderLayout.EAST);
            panel.add(bottom, BorderLayout.SOUTH);

            plus.addActionListener(e -> changeQty(1));
            minus.addActionListener(e -> changeQty(-1));
            remove.addActionListener(e -> {
                int id = selectedCartId();
                if (id < 0) return;
                cart.remove(id);
                refreshCart();
            });
            clearBtn.addActionListener(e -> { cart.clear(); refreshCart(); });
            checkout.addActionListener(e -> checkout());
            return panel;
        }

        private int selectedCartId() {
            int row = cartTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select an item in the cart first.");
                return -1;
            }
            return (Integer) cartModel.getValueAt(row, 0);
        }

        private void changeQty(int delta) {
            int id = selectedCartId();
            if (id < 0) return;
            int q = cart.get(id) + delta;
            if (q <= 0) cart.remove(id); else cart.put(id, q);
            refreshCart();
        }

        private void addToCart(Product p) {
            cart.merge(p.id, 1, Integer::sum);
            refreshCart();
            status.setText(p.name + " added to cart (" + cart.get(p.id) + " in cart)");
        }

        private double refreshCart() {
            cartModel.setRowCount(0);
            double total = 0;
            int items = 0;
            for (Map.Entry<Integer, Integer> en : cart.entrySet()) {
                Product p = findProduct(en.getKey());
                double sub = p.price * en.getValue();
                total += sub;
                items += en.getValue();
                cartModel.addRow(new Object[]{p.id, p.name, String.format("%.2f", p.price),
                        en.getValue(), String.format("%.2f", sub)});
            }
            totalLbl.setText(String.format("Total: Rs. %.2f", total));
            tabs.setTitleAt(1, items == 0 ? "Cart" : "Cart (" + items + ")");
            return total;
        }

        // ---------- checkout ----------
        private void checkout() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your cart is empty.");
                return;
            }
            double total = refreshCart();

            JTextField nameF = new JTextField(currentUser, 18);
            JTextField addrF = new JTextField(18);
            JComboBox<String> pay = new JComboBox<>(new String[]{"Cash on Delivery", "UPI", "Credit/Debit Card"});
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
            form.add(new JLabel("Full name:"));      form.add(nameF);
            form.add(new JLabel("Address:"));        form.add(addrF);
            form.add(new JLabel("Payment:"));        form.add(pay);
            form.add(new JLabel("Amount payable:")); form.add(new JLabel(String.format("Rs. %.2f", total)));

            int res = JOptionPane.showConfirmDialog(this, form, "Checkout",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            if (nameF.getText().trim().isEmpty() || addrF.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and address are required.",
                        "Checkout", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int id = nextOrderId++;
            StringBuilder sb = new StringBuilder();
            sb.append("=========== SHOPEASY INVOICE ===========\n");
            sb.append("Order No : ").append(id).append("\n");
            sb.append("Date     : ").append(new Date()).append("\n");
            sb.append("Customer : ").append(nameF.getText().trim()).append("\n");
            sb.append("Ship to  : ").append(addrF.getText().trim()).append("\n");
            sb.append("Payment  : ").append(pay.getSelectedItem()).append("\n");
            sb.append("----------------------------------------\n");
            for (Map.Entry<Integer, Integer> en : cart.entrySet()) {
                Product p = findProduct(en.getKey());
                sb.append(String.format("%-24s %2d x %8.2f = %9.2f%n",
                        p.name, en.getValue(), p.price, p.price * en.getValue()));
            }
            sb.append("----------------------------------------\n");
            sb.append(String.format("TOTAL                                Rs. %.2f%n", total));
            sb.append("========================================\n");
            sb.append("Thank you for shopping with ShopEasy!\n");

            orders.add(new Order(id, currentUser, sb.toString(), total));
            cart.clear();
            refreshCart();
            refreshOrders();
            orderList.setSelectedIndex(orderListModel.size() - 1);
            tabs.setSelectedIndex(2);
            status.setText("Order #" + id + " placed successfully.");
            JOptionPane.showMessageDialog(this, "Order #" + id + " placed successfully!");
        }

        // ---------- orders tab ----------
        private JPanel buildOrdersTab() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            orderDetail.setEditable(false);
            orderDetail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            orderDetail.setText("Select an order on the left to view its invoice.");
            orderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            orderList.addListSelectionListener((ListSelectionEvent e) -> {
                if (e.getValueIsAdjusting()) return;
                Order o = orderList.getSelectedValue();
                if (o != null) orderDetail.setText(o.text);
            });

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(orderList), new JScrollPane(orderDetail));
            split.setDividerLocation(260);
            panel.add(split, BorderLayout.CENTER);

            JButton save = btn("Save Invoice as .txt", PRIMARY);
            save.addActionListener(e -> saveInvoice());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(save);
            panel.add(south, BorderLayout.SOUTH);
            return panel;
        }

        private void refreshOrders() {
            orderListModel.clear();
            for (Order o : orders) if (o.user.equals(currentUser)) orderListModel.addElement(o);
        }

        private void saveInvoice() {
            Order o = orderList.getSelectedValue();
            if (o == null) {
                JOptionPane.showMessageDialog(this, "Select an order first.");
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("invoice_" + o.id + ".txt"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter w = new PrintWriter(fc.getSelectedFile())) {
                    w.print(o.text);
                    status.setText("Invoice saved to " + fc.getSelectedFile().getName());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Could not save file: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // ---------- admin tab ----------
        private JPanel buildAdminTab() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel title = new JLabel("Admin - Manage Products");
            title.setFont(new Font("SansSerif", Font.BOLD, 22));
            title.setForeground(PRIMARY);
            panel.add(title, BorderLayout.NORTH);

            adminTable.setRowHeight(26);
            adminTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            panel.add(new JScrollPane(adminTable), BorderLayout.CENTER);

            JTextField nameF = new JTextField(14), catF = new JTextField(10), priceF = new JTextField(7);
            JButton add = btn("Add Product", ACCENT), del = btn("Delete Selected", DANGER);
            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            form.add(new JLabel("Name:"));     form.add(nameF);
            form.add(new JLabel("Category:")); form.add(catF);
            form.add(new JLabel("Price:"));    form.add(priceF);
            form.add(add); form.add(del);
            panel.add(form, BorderLayout.SOUTH);

            add.addActionListener(e -> {
                String n = nameF.getText().trim(), c = catF.getText().trim();
                double pr;
                try {
                    pr = Double.parseDouble(priceF.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Enter a valid price.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (n.isEmpty() || c.isEmpty() || pr <= 0) {
                    JOptionPane.showMessageDialog(this, "Fill all fields with valid values.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                addProduct(n, c, pr);
                nameF.setText(""); catF.setText(""); priceF.setText("");
                refreshAll();
                status.setText("Product added: " + n);
            });
            del.addActionListener(e -> {
                int row = adminTable.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Select a product to delete.");
                    return;
                }
                int id = (Integer) adminModel.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    products.remove(findProduct(id));
                    cart.remove(id);
                    refreshAll();
                    status.setText("Product deleted.");
                }
            });
            return panel;
        }

        private void refreshAdmin() {
            adminModel.setRowCount(0);
            for (Product p : products)
                adminModel.addRow(new Object[]{p.id, p.name, p.category, String.format("%.2f", p.price)});
        }

        private void refreshAll() {
            refreshCategories();
            refreshShop();
            refreshCart();
            refreshAdmin();
        }
    }

    // =====================================================================
    public static void main(String[] args) {
        seed();
        EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
