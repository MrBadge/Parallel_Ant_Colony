package GUI;


import ACOSolver.AntColony;
import Common.Customer;
import Common.Depot;
import Common.Place;
import Common.Vehicle;
import MDVRPTW.MDVRPTW;
import com.anatolymaltsev.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class MainPanel extends JFrame
        implements /*MouseListener, MouseMotionListener,*/ Runnable {

    private static final Font font = new Font("Dialog", Font.BOLD, 12);
    private static final Font small = new Font("Dialog", Font.PLAIN, 10);

    private JScrollPane scroll = null;
    private PanelLogic panel = null;
    private JTextField stat = null;
    private JDialog randvrp = null;
    private JDialog antcol = null;
    private JDialog runopt = null;
    private JDialog params = null;
    //private JDialog about = null;
    private JFileChooser chooser = null;
    private MDVRPTW mdvrptw = null;
    private Timer timer = null;
    private int epochs = -1;

    public static int panelDefHeight = 700;
    public static int panelDefWidth = 700;

    private JFileChooser createChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileHidingEnabled(true);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setMultiSelectionEnabled(false);
        fc.setFileView(null);
        return fc;
    }


    private void genMDVRP(int customersCount, int depotsCount/*, long seed*/) {
        if (this.epochs >= 0) return;
        //Random rand = (seed > 0) ? new Random(seed) : new Random();
        this.mdvrptw = new MDVRPTW(depotsCount, customersCount);
        Point2D.Double maxPoint = this.mdvrptw.getPlaces().findMaxPoint();
        Double min = Math.min(maxPoint.getX(), maxPoint.getY());
        this.mdvrptw.getPlaces().transform(Math.min(panelDefHeight, panelDefWidth) / min * 0.9, 5, 5);
        this.panel.setMDVRP(MainPanel.this.mdvrptw);
        this.repaint();
        this.stat.setText("Случайная задача MDVRPTW сгенерирована");
    }

    private void loadMDVRP(File file, String data_type) {
        if (this.epochs >= 0) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ArrayList<Depot> depots = new ArrayList<>();
            ArrayList<Customer> customers = new ArrayList<>();
            int vehicleLinesLeft = 0;
            String[] depotLine = new String[0];
            ArrayList<Vehicle> vehiclesAcc = new ArrayList<>();
            if (Objects.equals(data_type, "coord")) {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("(( |\t)+)");
                    if (vehicleLinesLeft > 0) {
                        vehiclesAcc.add(new Vehicle(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                        if (--vehicleLinesLeft == 0) {
                            depots.add(new Depot(Integer.parseInt(depotLine[0]), Double.parseDouble(depotLine[1]), Double.parseDouble(depotLine[2]),
                                    Double.parseDouble(depotLine[4]), Double.parseDouble(depotLine[5]), Double.parseDouble(depotLine[6]),
                                    new ArrayList<>(vehiclesAcc)));
                        }
                        continue;
                    }
                    if (parts.length <= 8) {
                        depotLine = parts;
                        vehicleLinesLeft = Integer.parseInt(parts[7]);
                        vehiclesAcc.clear();
                    } else {
                        customers.add(new Customer(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                                Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                                Double.parseDouble(parts[6]), parts[7].equals("1"), Double.parseDouble(parts[8])));
                    }

                }
            } else {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || Objects.equals(line, "")) {
                        continue;
                    }
                    String[] parts = line.split("(( |\t)+;( |\t)+)");
                    if (vehicleLinesLeft > 0) {
                        vehiclesAcc.add(new Vehicle(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                        if (--vehicleLinesLeft == 0) {
                            depots.add(new Depot(Integer.parseInt(depotLine[0]), depotLine[1], Double.parseDouble(depotLine[3]),
                                    Double.parseDouble(depotLine[4]), Double.parseDouble(depotLine[5]), new ArrayList<>(vehiclesAcc)));
                        }
                        continue;
                    }
                    if (parts.length <= 7) {
                        depotLine = parts;
                        vehicleLinesLeft = Integer.parseInt(parts[6]);
                        vehiclesAcc.clear();
                    } else {
                        customers.add(new Customer(Integer.parseInt(parts[0]), parts[1],
                                Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                                Double.parseDouble(parts[5]), parts[6].equals("1"), Double.parseDouble(parts[7])));
                    }

                }
            }
            this.mdvrptw = new MDVRPTW(depots, customers);
            Point2D.Double maxPoint = this.mdvrptw.getPlaces().findMaxPoint();
            Point2D.Double minPoint = this.mdvrptw.getPlaces().findMinPoint();
            Double scaleX = panelDefWidth / (maxPoint.getX() - minPoint.getX());
            Double scaleY = panelDefHeight / (maxPoint.getY() - minPoint.getY());
            Double scale_factor = Math.min(scaleX, scaleY) * 0.7;
            Point2D.Double centralPoint = new Point2D.Double(maxPoint.getX() - minPoint.getX(), maxPoint.getY() - minPoint.getY());
            Point2D.Double translation = new Point2D.Double(panelDefWidth / 5 - centralPoint.getX(), panelDefHeight / 5 - centralPoint.getY());
            this.mdvrptw.getPlaces().transform(translation, scale_factor, minPoint);
            this.panel.setMDVRP(MainPanel.this.mdvrptw);
            this.repaint();
            this.stat.setText("Задача MDVRPTW создана");
        } catch (IOException e) {
            this.stat.setText("Формат файла неверен");
        }
    }

    private JDialog createRandMDVRP() {
        final JDialog dlg = new JDialog(this,
                "Создать случайную задачу MDVRPTW...");
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints lc = new GridBagConstraints();
        GridBagConstraints rc = new GridBagConstraints();
        JPanel grid = new JPanel(g);
        JPanel bbar;
        JLabel lbl;
        JTextArea help;
        JButton btn;

        grid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lc.fill = rc.fill = GridBagConstraints.BOTH;
        rc.weightx = 1.0;
        lc.weightx = 0.0;
        lc.weighty = 0.0;
        rc.weighty = 0.0;
        lc.ipadx = 10;
        lc.ipady = 10;
        rc.gridwidth = GridBagConstraints.REMAINDER;

        lbl = new JLabel("Количество клиентов:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JSpinner customersCount = new JSpinner(
                new SpinnerNumberModel(25, 1, 999999, 1));
        g.setConstraints(customersCount, rc);
        grid.add(customersCount);

        lbl = new JLabel("Количество депо:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JSpinner depotsCount = new JSpinner(
                new SpinnerNumberModel(1, 1, 999999, 1));
        g.setConstraints(depotsCount, rc);
        grid.add(depotsCount);

        /*lbl = new JLabel("Seed for random numbers:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JSpinner seed = new JSpinner(
                new SpinnerNumberModel(0, 0, 999999, 1));
        g.setConstraints(seed, rc);
        grid.add(seed);*/

//        help = new JTextArea(
//                "Если сид для ГПСЧ равен 0,\n"
//                        + "в качестве него будет использовано системное время");
//        help.setFont(small);
//        help.setEditable(false);
//        help.setBackground(this.getBackground());
//        g.setConstraints(help, rc);
//        grid.add(help);

        bbar = new JPanel(new GridLayout(1, 2, 5, 5));
        bbar.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 3));
        btn = new JButton("Готово");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
                MainPanel.this.genMDVRP
                        (
                                (Integer) customersCount.getValue(),
                                (Integer) depotsCount.getValue()
                                //((Integer) seed.getValue()).longValue()
                        );
            }
        });
        btn = new JButton("Применить");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.genMDVRP
                        (
                                (Integer) customersCount.getValue(),
                                (Integer) depotsCount.getValue()
                                //((Integer) seed.getValue()).longValue()
                        );
            }
        });
        btn = new JButton("Закрыть");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });

        dlg.getContentPane().add(grid, BorderLayout.CENTER);
        dlg.getContentPane().add(bbar, BorderLayout.SOUTH);
        dlg.setLocationRelativeTo(this);
        dlg.setLocation(664, 0);
        dlg.pack();
        return dlg;
    }

    private JDialog createAnts() {
        final JDialog dlg = new JDialog(this, "Создать муравьиную колонию...");
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints lc = new GridBagConstraints();
        GridBagConstraints rc = new GridBagConstraints();
        JPanel grid = new JPanel(g);
        JPanel bbar;
        JLabel lbl;
        JTextArea help;
        JButton btn;

        grid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lc.fill = rc.fill = GridBagConstraints.BOTH;
        rc.weightx = 1.0;
        lc.weightx = 0.0;
        lc.weighty = 0.0;
        rc.weighty = 0.0;
        lc.ipadx = 10;
        lc.ipady = 10;
        rc.gridwidth = GridBagConstraints.REMAINDER;


//        lbl = new JLabel("Seed for random numbers:");
//        g.setConstraints(lbl, lc);
//        grid.add(lbl);
//        final JSpinner seed = new JSpinner(
//                new SpinnerNumberModel(0, 0, 999999, 1));
//        g.setConstraints(seed, rc);
//        grid.add(seed);
//
//        help = new JTextArea(
//                "If the seed for the pseudo-random number generator\n"
//                        + "is set to zero, the system time will be used instead.");
//        help.setFont(small);
//        help.setEditable(false);
//        help.setBackground(this.getBackground());
//        g.setConstraints(help, rc);
//        grid.add(help);

        lbl = new JLabel("Начальное значение феромона:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JTextField phinit = new JTextField("0");
        phinit.setFont(font);
        g.setConstraints(phinit, rc);
        grid.add(phinit);

        lbl = new JLabel("Вероятность неслучайного выбора дуги:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JTextField exploit = new JTextField("0.75");
        exploit.setFont(font);
        g.setConstraints(exploit, rc);
        grid.add(exploit);

//        lbl = new JLabel("Вес следа феромона:");
//        g.setConstraints(lbl, lc);
//        grid.add(lbl);
//        final JTextField alpha = new JTextField("1");
//        alpha.setFont(font);
//        g.setConstraints(alpha, rc);
//        grid.add(alpha);

//        lbl = new JLabel("Вес обратного расстояния:");
//        g.setConstraints(lbl, lc);
//        grid.add(lbl);
//        final JTextField beta = new JTextField("1");
//        beta.setFont(font);
//        g.setConstraints(beta, rc);
//        grid.add(beta);

        lbl = new JLabel("Скорость испарения:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JTextField evap = new JTextField("0.1");
        evap.setFont(font);
        g.setConstraints(evap, rc);
        grid.add(evap);

        lbl = new JLabel("Экспонента лок. обновления следа:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JTextField layexp = new JTextField("1");
        layexp.setFont(font);
        g.setConstraints(layexp, rc);
        grid.add(layexp);

        /*lbl = new JLabel("Elite enhancement:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JTextField elite = new JTextField("0.1");
        elite.setFont(font);
        g.setConstraints(elite, rc);
        grid.add(elite);*/

        bbar = new JPanel(new GridLayout(1, 2, 5, 5));
        bbar.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 3));
        btn = new JButton("Ok");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
                //int s = ((Integer) seed.getValue()).intValue();
                MainPanel.this.panel.initAnts(
                        //((Integer) antcnt.getValue()).intValue(),
                        Double.parseDouble(phinit.getText())//,
                        //(s != 0) ? new Random(s) : new Random()
                );
                MainPanel.this.panel.setParams(
                        Double.parseDouble(exploit.getText()),
//                        Double.parseDouble(alpha.getText()),
//                        Double.parseDouble(beta.getText()),
                        Double.parseDouble(layexp.getText()),
                        //Double.parseDouble(elite.getText()),
                        Double.parseDouble(evap.getText()));
            }
        });
        btn = new JButton("Применить");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //int s = ((Integer) seed.getValue()).intValue();
                MainPanel.this.panel.initAnts(
                        //((Integer) antcnt.getValue()).intValue(),
                        Double.parseDouble(phinit.getText())//,
                        //(s != 0) ? new Random(s) : new Random()
                );
                MainPanel.this.panel.setParams(
                        Double.parseDouble(exploit.getText()),
//                        Double.parseDouble(alpha.getText()),
//                        Double.parseDouble(beta.getText()),
                        Double.parseDouble(layexp.getText()),
                        //Double.parseDouble(elite.getText()),
                        Double.parseDouble(evap.getText()));
            }
        });
        btn = new JButton("Закрыть");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });

        dlg.getContentPane().add(grid, BorderLayout.CENTER);
        dlg.getContentPane().add(bbar, BorderLayout.SOUTH);
        dlg.setLocationRelativeTo(this);
        dlg.setLocation(664, 145);
        dlg.pack();
        return dlg;
    }

    private void runVehicles(int epochs, int delay) {
        /*if (this.epochs >= 0) {
            this.timer.stop();
            this.epochs = -1;
            return;
        }*/
        //AntColony ants = this.panel.getAnts();
        if (this.panel.getAnts() == null) return;
        /*if (delay <= 0) {
            while (--epochs >= 0)
                this.panel.runEpoch();
            this.panel.repaint();
            this.stat.setText("epoch: " + ants.getEpoch()
                    + ", best tour: " + ants.getBestTourLen());
            return;
        }*/
        this.epochs = epochs;
        this.timer = new Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.epochs -= 2;
                if (MainPanel.this.epochs < 0) {
                    MainPanel.this.timer.stop();
                    return;
                }
                MainPanel.this.panel.runEpoch();
//                MainPanel.this.panel.repaint();
                ArrayList<AntColony> ants = MainPanel.this.panel.getAnts();
                int curEpoch = ants.get(0).getEpoch();
                double bestTourLen = 0;
                int clientsVisited = 0;
                for (AntColony ac : ants) {
                    bestTourLen += ac.getBestTourLen();
                    clientsVisited += ac.getClientsCountVisited();
                }
                MainPanel.this.stat.setText(String.format("Эпоха: %d, длина лучшего пути: %.2f м, клиентов посещено: %d/%d", curEpoch, bestTourLen, clientsVisited, mdvrptw.getClientsCount()));
            }
        });
        this.timer.start();
    }

    private JDialog createRunOpt() {
        final JDialog dlg = new JDialog(this, "Запустить оптимизацию...");
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints lc = new GridBagConstraints();
        GridBagConstraints rc = new GridBagConstraints();
        JPanel grid = new JPanel(g);
        JPanel bbar;
        JLabel lbl;
        //JTextArea help;
        JButton btn;

        grid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lc.fill = rc.fill = GridBagConstraints.BOTH;
        rc.weightx = 1.0;
        lc.weightx = 0.0;
        lc.weighty = 0.0;
        rc.weighty = 0.0;
        lc.ipadx = 10;
        lc.ipady = 10;
        rc.gridwidth = GridBagConstraints.REMAINDER;

        lbl = new JLabel("Количество эпох:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JSpinner epochs = new JSpinner(
                new SpinnerNumberModel(15000, 1, 999999, 1000));
        g.setConstraints(epochs, rc);
        grid.add(epochs);

        lbl = new JLabel("Задержка между эпохами:");
        g.setConstraints(lbl, lc);
        grid.add(lbl);
        final JSpinner delay = new JSpinner(
                new SpinnerNumberModel(5, 0, 999999, 5));
        g.setConstraints(delay, rc);
        grid.add(delay);

        bbar = new JPanel(new GridLayout(1, 2, 5, 5));
        bbar.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 3));
        btn = new JButton("Ok");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
                MainPanel.this.runVehicles((Integer) epochs.getValue(),
                        (Integer) delay.getValue());
            }
        });
        btn = new JButton("Применить");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.runVehicles((Integer) epochs.getValue(),
                        (Integer) delay.getValue());
            }
        });
        btn = new JButton("Закрыть");
        bbar.add(btn);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });

        dlg.getContentPane().add(grid, BorderLayout.CENTER);
        dlg.getContentPane().add(bbar, BorderLayout.SOUTH);
        dlg.setLocationRelativeTo(this);
        dlg.setLocation(664, 465);
        dlg.pack();
        return dlg;
    }

    @Override
    public void run() {
        JMenuBar mbar;
        JMenu menu;
        JMenuItem item;

        this.getContentPane().setLayout(new BorderLayout());

        mbar = new JMenuBar();
        this.getContentPane().add(mbar, BorderLayout.NORTH);

        menu = mbar.add(new JMenu("Файл"));
        menu.setMnemonic('f');

        item = menu.add(new JMenuItem("Загрузить задачу с координатами..."));
        item.setMnemonic('l');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.loadTask(null, "coord");
            }
        });
        item = menu.add(new JMenuItem("Загрузить задачу с адресами..."));
        item.setMnemonic('a');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.loadTask(null, "addr");
            }
        });
        item = menu.add(new JMenuItem("Сохранить как PNG..."));
        item.setMnemonic('i');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.saveImage(null);
            }
        });
        menu.addSeparator();
//        item = menu.add(new JMenuItem("Сохранить заачу ..."));
//        item.setMnemonic('s');
//        item.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                MainPanel.this.saveResults(null);
//            }
//        });
//        menu.addSeparator();
        item = menu.add(new JMenuItem("Выйти"));
        item.setMnemonic('q');

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });


        menu = mbar.add(new JMenu("Действия"));
        menu.setMnemonic('a');
        item = menu.add(new JMenuItem("Создать случайную задачу MDVRPTW..."));
        item.setMnemonic('g');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainPanel.this.randvrp == null)
                    MainPanel.this.randvrp = createRandMDVRP();
                MainPanel.this.randvrp.setVisible(true);
            }
        });
        item = menu.add(new JMenuItem("Создать муравьиную колонию..."));
        item.setMnemonic('c');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainPanel.this.antcol == null)
                    MainPanel.this.antcol = createAnts();
                MainPanel.this.antcol.setVisible(true);
            }
        });
        item = menu.add(new JMenuItem("Запустить оптимизацию..."));
        item.setMnemonic('o');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainPanel.this.runopt == null)
                    MainPanel.this.runopt = createRunOpt();
                MainPanel.this.runopt.setVisible(true);
            }
        });
        item = menu.add(new JMenuItem("Остановить оптимизацию"));
        item.setMnemonic('s');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainPanel.this.timer == null) return;
                MainPanel.this.timer.stop();
                MainPanel.this.epochs = -1;
            }
        });
        /*menu.addSeparator();
        item = menu.add(new JMenuItem("Redraw"));
        item.setMnemonic('r');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.panel.repaint();
            }
        });*/

        this.panel = new PanelLogic();
        this.panel.setLayout(new BorderLayout());
        this.panel.setPreferredSize(new Dimension(panelDefWidth, panelDefHeight));
        //this.panel.addMouseListener(this);
        //this.panel.addMouseMotionListener(this);
        this.scroll = new JScrollPane(this.panel);
        this.getContentPane().add(this.scroll, BorderLayout.CENTER);

        this.stat = new JTextField("");
        this.stat.setEditable(false);
        this.getContentPane().add(this.stat, BorderLayout.SOUTH);

        this.setTitle("Решение задачи маршрутизации транспорта с множеством депо");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //this.setLocation(0, 0);
        this.setLocationRelativeTo(null);
        //this.setLocation(this.getX() - this.getWidth() / 2, this.getY() - this.getHeight() / 2);
        this.pack();
        this.setVisible(true);
        this.stat.setText("Программа готова, ожидание действия...");
    }

    private void loadTask(File file, String data_type) {
        if (file == null) {
            if (this.chooser == null) this.chooser = this.createChooser();
            this.chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            int r = this.chooser.showDialog(this, null);
            if (r != JFileChooser.APPROVE_OPTION) return;
            file = this.chooser.getSelectedFile();
        }
        loadMDVRP(file, data_type);
    }

    public void saveResults(File file) {
        if (file == null) {
            if (this.chooser == null) this.chooser = this.createChooser();
            this.chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int r = this.chooser.showDialog(this, null);
            if (r != JFileChooser.APPROVE_OPTION) return;
            file = this.chooser.getSelectedFile();
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            ImageIO.write(this.panel.makeImage(), "txt", stream);
            stream.close();
        } catch (IOException e) {
            String msg = e.getMessage();
            stat.setText(msg);
            System.err.println(msg);
            JOptionPane.showMessageDialog(this, msg,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveImage(File file) {
        if (file == null) {
            if (this.chooser == null) this.chooser = this.createChooser();
            this.chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            int r = this.chooser.showDialog(this, null);
            if (r != JFileChooser.APPROVE_OPTION) return;
            file = this.chooser.getSelectedFile();
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            ImageIO.write(this.panel.makeImage(), "png", stream);
            stream.close();
        } catch (IOException e) {
            String msg = e.getMessage();
            stat.setText(msg);
            System.err.println(msg);
            JOptionPane.showMessageDialog(this, msg,
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public MainPanel() {
        try {
            EventQueue.invokeAndWait(this);
        } catch (Exception e) {
        }
    }

    public static void main(String args[]) {
        new MainPanel();
    }
}
