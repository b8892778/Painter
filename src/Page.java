
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Page extends JPanel {
    private Point p1, p2;
    private Color PenColor, EraserColor;
    private Stroke PenStroke;
    private Shape shape = null;
    private int Start, lineWidth;
    private final ArrayList<DrawObjects> shapeList = new ArrayList();
    private final ArrayList<DrawObjects> freeList = new ArrayList();
    private boolean CtrlDown = false; 
    public boolean isFill = false; 
    public Status status;
    
    Page(MainWindow parant) {
        this.setBackground(Color.WHITE);
        this.setLayout(null);
        this.addMouseListener(new myMouseAdapter());
        this.addMouseMotionListener(new myMouseAdapter());
        this.addKeyListener(new myKeyAdapter());
        lineWidth = 4;
        status = Status.Pen;
        PenStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (DrawObjects temp : shapeList) {
            g2d.setStroke(temp.stroke);
            g2d.setColor(temp.color);
            if(temp.isFill)
                g2d.fill(temp.s);
            g2d.draw(temp.s);
        }
        if (shape != null && status != Status.Eraser) { //畫出拖曳軌跡
            g2d.setStroke(PenStroke);
            g2d.setColor(PenColor);
            if(isFill)
                g2d.fill(shape);
            g2d.draw(shape);
        }
    }
    
    public void Undo() {
        shape = null;
        int f_size = freeList.size() - 1;
        if (freeList.size() > 0 && shapeList.size() == freeList.get(f_size).end) {
            int i = freeList.get(f_size).start;
            int j = freeList.get(f_size).end - 1;
            for (; j > i; j--) {
                shapeList.remove(j);
            }
            freeList.remove(f_size);
        }
        if (shapeList.size() > 0) {
            shapeList.remove(shapeList.size() - 1);
        }
        repaint();
    }
    
    public void  ChooseColor() {
        Color c = JColorChooser.showDialog(this, "選擇顏色", getBackground());
        if (c != null) {
            if(ToolBar.colorJTBtn[0].isSelected()) {
                ToolBar.colorPanel[0].setBackground(c);
            } else if(ToolBar.colorJTBtn[1].isSelected()){
                ToolBar.colorPanel[1].setBackground(c);
            }
        }
    }
    
    public void  SetStroke(int lineWidth) {
        this.lineWidth = lineWidth;
        PenStroke = new BasicStroke(this.lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
        this.requestFocus();
    }
    
    class myKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown())
                Undo();
            if(e.getKeyCode() == KeyEvent.VK_ADD && e.isControlDown()&& lineWidth < 30)
                SetStroke(lineWidth+1);
            if(e.getKeyCode() == KeyEvent.VK_SUBTRACT && e.isControlDown() && lineWidth > 0)
                SetStroke(lineWidth-1);
            if(e.isControlDown())
                CtrlDown = true;
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if(!e.isControlDown())
                CtrlDown = false;
        }
    }
    
    class myMouseAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            p1 = e.getPoint();
            PenColor = ToolBar.colorPanel[0].getBackground();
            EraserColor = ToolBar.colorPanel[1].getBackground();
            
            if(status == Status.Pen || status == Status.Eraser) {
                Start =  shapeList.size();
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            p2 = e.getPoint();
            int width = Math.abs(p2.x - p1.x);
            int height = Math.abs(p2.y - p1.y);
            
            if (CtrlDown)
                height = width;

            switch (status) {
                case Pen:
                    shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                    p1 = p2;
                    shapeList.add(new DrawObjects(shape, PenColor, PenStroke, false));
                    break;
                case Eraser:
                    shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                    p1 = p2;
                    shapeList.add(new DrawObjects(shape, EraserColor, PenStroke, false));
                    break;
                case Line:
                    shape = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
                    break;
                case Rectangle:
                    shape = new Rectangle2D.Double(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), width, height);
                    break;
                case Round_Rectangle:
                    shape = new RoundRectangle2D.Double(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), width, height, 30, 30);
                    break;
                case Oval:
                    shape = new Ellipse2D.Double(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), width, height);
                    break;
            }
            repaint();
            MainWindow.statusBar.setText("滑鼠座標: (" + e.getX() + "," + e.getY() + ")");
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            switch (status) {
                case Pen:
                case Eraser:
                    freeList.add(new DrawObjects(Start, shapeList.size()));
                    break;
                case Line:
                case Rectangle:
                case Round_Rectangle:
                case Oval:
                    shapeList.add(new DrawObjects(shape, PenColor, PenStroke, isFill));
                    repaint();
                    break;
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            MainWindow.statusBar.setText("滑鼠座標: (" + e.getX() + "," + e.getY() + ")");
        }
    }

    public void NewPage() {
        shapeList.removeAll(shapeList);
        shape = null;
        repaint();
    }
    
    public void Open() {
        JFileChooser Open_JC = new JFileChooser();
        Open_JC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        Open_JC.setDialogTitle("開啟檔案");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "jpg", "gif", "png");
        Open_JC.setFileFilter(filter);
        int result = Open_JC.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = Open_JC.getSelectedFile();
            /*try {
                image = ImageIO.read(new File(file.getAbsolutePath()));
                repaint();
            } catch (IOException e) {
            }*/
        }
    }
    
    public void Save() {
        JFileChooser Save_JC = new JFileChooser();
        Save_JC.setFileSelectionMode(JFileChooser.SAVE_DIALOG | JFileChooser.DIRECTORIES_ONLY);
        Save_JC.setDialogTitle("儲存檔案");
        int result = Save_JC.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String path = Save_JC.getSelectedFile().getAbsolutePath();
            BufferedImage image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            this.paint(g);
            if (path != null) {
                try {
                    File file = new File(path, "未命名.png");
                    ImageIO.write(image, "png", file);
                } catch (IOException ex) {
                    Logger.getLogger(Page.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
