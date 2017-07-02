package GUI;

import ACOSolver.AntColony;
import Common.Customer;
import Common.Depot;
import Common.Place;
import Common.Vehicle;
import Helpers.Tuple;
import MDVRPTW.MDVRPTW;
import SDVRPTW.SDVRPTW;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.text.NumberFormat;

public class PanelLogic extends JPanel {

    private NumberFormat defaultFormat = NumberFormat.getPercentInstance();

    public ArrayList<AntColony> getAnts() {
        return antColonies;
    }

    private class P2D {
        public int x;
        public int y;

        public P2D(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }


    private MDVRPTW mdvrptw;
    private ArrayList<AntColony> antColonies;

    private Stroke line = new BasicStroke(2.0F);
    private Color[] colors = new Color[]{
            Color.orange,
            Color.green,
            Color.blue,
            Color.red,
            Color.magenta
    };

    public void setMDVRP(MDVRPTW mdvrptw) {
        this.antColonies = null;
        this.mdvrptw = mdvrptw;
        this.revalidate();
        this.repaint();
    }

    public void setParams(double exploit, /*double alpha, double beta,*/
                          double trail,/* double elite,*/ double evap) {
        if (this.antColonies == null) return;
        for (AntColony ac : this.antColonies) {
            ac.setExploit(exploit);
//            ac.setAlpha(alpha);
//            ac.setBeta(beta);
            ac.setTrail(trail);
            //this.antColonies.setElite(elite);
            ac.setEvap(evap);
        }
    }

    public void initAnts(double phero/*, Random rand*/) {
        if (this.mdvrptw == null) return;

        InputStream config;
        Properties settings = new Properties();
        try {
            config = new FileInputStream("config");
            settings.load(config);
        } catch (IOException ex) {
            settings.setProperty("pheromone_exp", "1");
            settings.setProperty("distance_exp", "2");
            settings.setProperty("f_exp", "0.4");
            settings.setProperty("w_exp", "0.8");
            settings.setProperty("savings_exp", "1");
            settings.setProperty("savings_heuristic_f_coeff", "2");
            settings.setProperty("savings_heuristic_g_coeff", "2");
            settings.setProperty("f_heuristic_delta_coeff", "0.01");
            try {
                OutputStream output = new FileOutputStream("config");
                settings.store(output, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.antColonies = new ArrayList<>();
        for (SDVRPTW sdvrptw : this.mdvrptw.getVrpSubProblems()) {
            AntColony tmp = new AntColony(sdvrptw, settings);
            tmp.init(phero);
            this.antColonies.add(tmp);
        }
        this.repaint();
    }

    public void runEpoch() {
        if (this.antColonies == null) return;
        boolean needToRedraw = false;
        for (AntColony ac : this.antColonies) {
            ac.runEpoch();
            if (ac.hasBetterSolution)
                needToRedraw = true;
        }
        if (needToRedraw)
            this.repaint();
    }

//    public void saveResults(File file) {
//        for (AntColony ac : this.antColonies) {
//            Map<Integer, ArrayList<Place>> solution = ac.getBestSolution();
//            if (solution != null) {
//
//            }
//        }
//    }

    public BufferedImage makeImage() {
        BufferedImage img;
        Dimension d;

        d = this.getPreferredSize();
        img = new BufferedImage(d.width, d.height,
                BufferedImage.TYPE_3BYTE_BGR);
        this.paint(img.getGraphics());
        return img;
    }

    public void paint(Graphics g) {
//        boolean needToRedraw = false;
//        if (antColonies != null)
//            for (AntColony ac : this.antColonies) {
//                if (ac.hasBetterSolution) {
//                    g.setColor(Color.white);
//                    g.fillRect(0, 0, MainPanel.panelDefWidth, MainPanel.panelDefHeight);
//                    needToRedraw = true;
//                    break;
//                }
//            }
        g.setColor(Color.white);
        g.fillRect(0, 0, MainPanel.panelDefWidth, MainPanel.panelDefHeight);

        if (this.mdvrptw == null)
            return;

        ((Graphics2D) g).setStroke(line);
        if (antColonies != null) {
            int delta_height = 0;
            for (AntColony ac : this.antColonies) {
                Map<Integer, Tuple<Vehicle, ArrayList<Place>>> routes = ac.getBestSolution();
                if (routes != null) {
                    int routes_count = 0;
                    for (Map.Entry<Integer, Tuple<Vehicle, ArrayList<Place>>> route : routes.entrySet()) {
                        ArrayList<Place> r = route.getValue().getRight();
                        if (r.size() < 2)
                            continue;
                        Vehicle v = route.getValue().getLeft();
                        g.setColor(colors[route.getKey() % colors.length]);
                        int i;
                        for (i = 0; i < route.getValue().getRight().size() - 1; ++i) {
                            P2D startPoint = new P2D((int) Math.round(r.get(i).getDisplayX()),
                                    (int) Math.round(r.get(i).getDisplayY()));
                            P2D endPoint = new P2D((int) Math.round(r.get(i + 1).getDisplayX()),
                                    (int) Math.round(r.get(i + 1).getDisplayY()));
                            g.drawLine(startPoint.x,
                                    startPoint.y,
                                    endPoint.x,
                                    endPoint.y);
                            if (i > 0)
                                g.drawString(route.getKey().toString() + "-" + Integer.toString(i),
                                        startPoint.x + 10, startPoint.y + 10);
                        }
                        P2D depoPoint = new P2D((int) Math.round(r.get(0).getDisplayX()),
                                (int) Math.round(r.get(0).getDisplayY()));
                        P2D lastPoint = new P2D((int) r.get(i).getDisplayX(), (int) r.get(i).getDisplayY());
                        g.drawString(route.getKey().toString() + "-" + Integer.toString(i),
                                lastPoint.x + 10,
                                lastPoint.y + 10);
                        g.drawLine(lastPoint.x,
                                lastPoint.y,
                                depoPoint.x,
                                depoPoint.y);

                        g.drawString("№" + route.getKey().toString() + ", Load: " + Double.toString(v.getCurrentCapacity()) + "/" + v.getMaxCapacity() +
                                        ", TW: [" + Double.toString(v.getTIS()) + ";" + Double.toString(v.getTIE()) + "], Clients: "
                                        + Integer.toString(r.size() - 1),
                                MainPanel.panelDefWidth - 350, (route.getKey()) * 15 + 20 + delta_height);
                        g.fillOval(MainPanel.panelDefWidth - 360, (route.getKey()) * 15 + 12 + delta_height, 7, 7);

                        routes_count += r.size();
                    }
                    delta_height += routes_count * 8;
                }

            }
        }
        int x, y;
        for (int i = 0; i < this.mdvrptw.getPlaces().getDepotsCount(); ++i) {
            Depot depot = this.mdvrptw.getDepot(i);
            x = (int) depot.getDisplayX();
            y = (int) depot.getDisplayY();
            g.setColor(Color.black);
            g.fillOval(x - 4, y - 4, 9, 9);
            g.setColor(Color.red);
            g.fillOval(x - 3, y - 3, 7, 7);
            g.drawString("[" + Double.toString(depot.getTIS()) + "; " + Double.toString(depot.getTIE()) + "]", x - 40, y + 25);
            g.drawString("vC: " + depot.getVehiclesCount(), x - 17, y + 40);
            if (depot.getAddress() != null)
                g.drawString(depot.getAddress(), x - 17, y + 55);
        }
        for (int i = 0; i < this.mdvrptw.getPlaces().getCustomersCount(); ++i) {
            Customer customer = this.mdvrptw.getCustomer(i);
            x = (int) customer.getDisplayX();
            y = (int) customer.getDisplayY();
            g.setColor(Color.black);
            g.fillOval(x - 4, y - 4, 9, 9);
            if (!customer.isReal()) {
                g.setColor(Color.darkGray);
            } else {
                g.setColor(Color.blue);
            }
            g.fillOval(x - 3, y - 3, 7, 7);
            g.drawString("[" + Double.toString(customer.getTIS()) + "; " + Double.toString(customer.getTIE()) + "]", x - 40, y + 25);
            g.drawString(String.format("№%s, AT:%.0f", customer.getNo(), customer.getArrivalTime()), x - 45, y + 40);
            if (customer.getAddress() != null) {
                g.drawString(String.format("%s", customer.getAddress()), x - 45, y + 55);
            }
            if (!customer.isReal()) {
                defaultFormat.setMinimumFractionDigits(1);
                g.drawString("Prob: " + defaultFormat.format(customer.getBecomeRealProbability()), x - 40, y + 55);
            }

        }

    }
}
