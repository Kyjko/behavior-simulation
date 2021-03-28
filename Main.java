import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Canvas {
    final JFrame f;
    final ArrayList<Sim> sims;
    final float chance_of_monster = 0.05f;
    final float chance_of_death_after_warning = 0.05f;
    final int N = 500;
    final int I = 2560;
    final int W = 2560;
    final int H = 500;

    final float dist = N/3.5f;

    final int[] current_coward_count;
    final int[] current_altruists_count;
    final int[] current_gbaltruists_count;

    boolean quit = false;

    enum simtype {
        Coward(),
        Altruistic(),
        GBAltruistic()
    }

    class Sim {
        int id;
        boolean is_alive;
        simtype type;
        boolean is_paired = false;
        boolean is_done_for_one_iter = false;

        Sim(int id, simtype type) {
            this.id = id;
            this.is_alive = true;
            this.type = type;
        }

        void doRoutine() {
            Random r = new Random();
            // get pair
            Sim peer;
            try {
                peer = sims.get(r.nextInt(sims.size()));
            } catch (Exception ex) {
                peer = null;
            }
            if (peer == null) {
                System.out.println("couldn't pair sim up with anybody!");
                return;
            }
            while (!peer.is_alive || peer.is_paired || peer.id == this.id) {
                try {
                    peer = sims.get(r.nextInt(sims.size()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (peer == null) {
                return;
            } else {
                this.is_paired = true;
            }

            this.is_done_for_one_iter = false;

            switch (this.type) {
                case Coward: {
                    if (this.is_alive) {
                        if (r.nextFloat() <= chance_of_monster) {
                            peer.is_alive = false;
                            peer.is_done_for_one_iter = true;

                        }
                        sims.add(new Sim(this.id + N, this.type));
                        this.is_done_for_one_iter = true;
                    }

                }
                case Altruistic: {
                    if (this.is_alive && !this.is_done_for_one_iter) {
                        if (r.nextFloat() <= chance_of_monster) {
                            if (!peer.is_done_for_one_iter) {
                                peer.is_alive = true;
                                peer.is_done_for_one_iter = true;
                                if (r.nextFloat() <= chance_of_death_after_warning) {
                                    this.is_alive = false;

                                } else {
                                    sims.add(new Sim(this.id + N, this.type));
                                }
                            }

                        } else {
                            sims.add(new Sim(this.id + N, this.type));
                        }
                        this.is_done_for_one_iter = true;
                    }

                }
                case GBAltruistic: {
                    if (this.is_alive && !this.is_done_for_one_iter) {
                        if (r.nextFloat() <= chance_of_monster) {
                            if (!peer.is_done_for_one_iter) {
                                if(peer.type == this.type) {
                                    peer.is_alive = true;
                                } else {
                                    peer.is_alive = false;
                                }
                                peer.is_done_for_one_iter = true;
                                if (r.nextFloat() <= chance_of_death_after_warning) {
                                    this.is_alive = false;

                                } else {
                                    sims.add(new Sim(this.id + N, this.type));
                                }
                            }

                        } else {
                            sims.add(new Sim(this.id + N, this.type));
                        }
                        this.is_done_for_one_iter = true;
                    }
                }
                default: {
                }

            }

        }
    }

    public void simulate() {
        Random r = new Random();

        for (int i = 0; i < N; i++) {
            if(i <= dist) {
                sims.add(new Sim(i, simtype.Coward));
            } else if(i >= dist && i <= 2*dist) {
                sims.add(new Sim(i, simtype.Altruistic));
            } else {
                sims.add(new Sim(i, simtype.GBAltruistic));
            }
        }

        for (int i = 0; i < I; i++) {
            int count_cowards = 0;
            int count_altruists = 0;
            int count_gbaltruists = 0;

            for (int j = 0; j < N; j++) {
                sims.get(j).doRoutine();
            }
            for (int j = 0; j < N; j++) {
                Sim s = sims.get(j);
                if (!s.is_alive) {
                    sims.remove(s);
                }
            }
            for (int j = 0; j < N; j++) {
                Sim s = sims.get(j);
                if (s.is_alive) {
                    if (s.type == simtype.Coward) {
                        count_cowards++;
                    } else if (s.type == simtype.Altruistic) {
                        count_altruists++;
                    } else if (s.type == simtype.GBAltruistic) {
                        count_gbaltruists++;
                    }
                }
            }

            //System.out.println("Cowards: " + count_cowards + " - " + "Altruists: " + count_altruists);
            current_coward_count[i] = count_cowards;
            current_altruists_count[i] = count_altruists;
            current_gbaltruists_count[i] = count_gbaltruists;
        }
    }

    public Main() {

        f = new JFrame("Altruistic simulator");
        f.setSize(W, H);
        f.setResizable(false);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.add(this);

        current_coward_count = new int[I];
        current_altruists_count = new int[I];
        current_gbaltruists_count = new int[I];

        sims = new ArrayList<Sim>(N);

        ExecutorService eS = Executors.newFixedThreadPool(2);
        eS.submit(this::simulate);
        eS.submit(this::eventloop);

        eS.shutdown();

    }

    public void eventloop() {
        while (!quit) {
            render();
        }
    }

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        g.setColor(Color.darkGray);
        g.fillRect(0, 0, W, H);

        int offset = 0;

        for (int i = 0; i < I; i++) {
            int cc = current_coward_count[i];
            int ca = current_altruists_count[i];
            int cga = current_gbaltruists_count[i];

            g.setColor(Color.orange);
            g.fillRect((int) (offset + ((float) W / (float) I) * i), 0, W / I, cc);
            g.setColor(Color.cyan);
            g.fillRect((int) (offset + ((float) W / (float) I) * i), cc, W / I, ca);
            g.setColor(Color.green);
            g.fillRect((int) (offset + ((float) W / (float) I) * i), ca+cc, W / I, cga);
        }

        g.dispose();
        bs.show();
    }

    public static void main(String[] args) {
        new Main();

    }
}
