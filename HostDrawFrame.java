package termproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

//TODO 문제를 맞추면 다음 라운드로 넘어가는 로직 추가

public class HostDrawFrame extends JFrame{
   private JPanel mainPanel, drawPanel, topPanel, sidePanel;
   private JButton penBtn, eraseBtn, colorBtn, clearBtn;
   private JLabel wordLabel, playerLabel, roundLabel;

   private final DrawingPanel drawingPanel;

   private final UdpSender sender;  // UDP로 그림 정보를 전송하는 객체 (Host → 클라이언트)
   
   public HostDrawFrame() {

       try {
           sender = new UdpSender("255.255.255.255", 9001); //그냥 예시로 해놓음
       } catch (Exception e) {
           throw new RuntimeException("UDP Sender 초기화 실패", e);
       }

      setTitle("CatchMind - 출제자");
      setSize(900, 600);
      setLocationRelativeTo(null);
      
      //sender 연결되는 생성자만 변경
      drawingPanel = new DrawingPanel(sender);
      
      mainPanel = new JPanel();
      mainPanel.setLayout(new BorderLayout());
      topPanel = new JPanel(new GridLayout(2, 1));
      
      //임시
      roundLabel = new JLabel("1/10");
      wordLabel = new JLabel("고래");
      
      topPanel.add(roundLabel);
      wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
      wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 22f));
      topPanel.add(wordLabel);
        
      mainPanel.add(topPanel, BorderLayout.NORTH);
      mainPanel.add(drawingPanel, BorderLayout.CENTER);
        
      sidePanel = new JPanel();
      sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
      penBtn = new JButton("펜");
      eraseBtn = new JButton("지우개");
      colorBtn = new JButton("색상");
      clearBtn = new JButton("모두 지우기");
        
      penBtn.addActionListener(e -> drawingPanel.setEraser(false));
      eraseBtn.addActionListener(e -> drawingPanel.setEraser(true));
      colorBtn.addActionListener(this::changeColor);
      clearBtn.addActionListener(e-> drawingPanel.clear());
        
      sidePanel.add(penBtn);
      sidePanel.add(Box.createVerticalStrut(10));
      sidePanel.add(eraseBtn);
      sidePanel.add(Box.createVerticalStrut(10));
      sidePanel.add(colorBtn);
      sidePanel.add(Box.createVerticalStrut(10));
      sidePanel.add(clearBtn);
        
      mainPanel.add(sidePanel, BorderLayout.EAST);
      mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
      add(mainPanel);
      setVisible(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }
   
    private void changeColor(ActionEvent e) {
         Color c = JColorChooser.showDialog(this, "색상 선택", drawingPanel.getCurrentColor());
         if (c != null) {
             drawingPanel.setCurrentColor(c);
         }
     }
    
    //UDP 송신기
    public class UdpSender{
        private final DatagramSocket socket;  // UDP 패킷 전송 소켓
        private final InetAddress address;    // 받을 대상 IP주소
        private final int port;               // 대상 포트 번호
        
        public UdpSender(String host, int port) throws Exception {
            this.socket = new DatagramSocket();
            this.address = InetAddress.getByName(host);
            this.port = port;
        }

        public void send(String msg) {
            try {
                byte[] buf = msg.getBytes(); 
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);  // UDP 패킷 전송
            } catch (Exception e) {
                System.out.println("UDP 전송 오류: " + e.getMessage());
            }
        }
    }
    
    
    
    private static class DrawingPanel extends JPanel {
       private BufferedImage canvas;
       private Graphics2D g2;
       private int lastX, lastY;   
       private boolean eraser = false;   
       private Color currentColor = Color.BLACK;

       private UdpSender sender;  //Host가 전달해준 UDP 송신기
      
       //새로운 생성자: sender만 연결하는 역할
       public DrawingPanel(UdpSender sender) {
           this.sender = sender;
           
           setPreferredSize(new Dimension(600, 400));
           setBackground(Color.WHITE);

           addMouseListener(new MouseAdapter() {
               @Override
               public void mousePressed(MouseEvent e) {
                   initGraphics();
                   lastX = e.getX();
                   lastY = e.getY();
               }
           });

           addMouseMotionListener(new MouseAdapter() {
               @Override
               public void mouseDragged(MouseEvent e) {
                   if (g2 == null) return;

                   int x = e.getX();
                   int y = e.getY();

                   int strokeWidth;  
                   Color drawColor;

                   if (eraser) {
                       strokeWidth = 20;
                       drawColor = Color.WHITE;
                   } else {
                       strokeWidth = 3;
                       drawColor = currentColor;
                   }

                   g2.setStroke(new BasicStroke(strokeWidth));
                   g2.setColor(drawColor);
                   g2.drawLine(lastX, lastY, x, y);

                   //UDP 전송
                   String msg = String.format(
                       "DRAW %d %d %d %d %d %d %d %d",
                       lastX, lastY,
                       x, y,
                       drawColor.getRed(),
                       drawColor.getGreen(),
                       drawColor.getBlue(),
                       strokeWidth
                   );
                   sender.send(msg);  //플레이어들에게 실시간 전송

                   lastX = x;
                   lastY = y;
                   repaint();
               }
           });
       }

       public DrawingPanel() {} 

        
       private void initGraphics() {
           if (canvas == null) {
              canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
              g2 = canvas.createGraphics();
              g2.setColor(Color.WHITE);
              g2.fillRect(0, 0, getWidth(), getHeight());
              g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           }
        }
        
        public void clear() {
           if (canvas != null && g2 != null) {
              g2.setColor(Color.WHITE);
              g2.fillRect(0, 0, getWidth(), getHeight());
              repaint();
            }
        }
        
        public void setEraser(boolean eraser) {
           this.eraser = eraser;
        }
        
        public void setCurrentColor(Color currentColor) {
           this.currentColor = currentColor;
        }
        
        public Color getCurrentColor() {
           return currentColor;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
           super.paintComponent(g);
            if (canvas != null) {
               g.drawImage(canvas, 0, 0, null);
           }
        }
    }
    
    public static void main(String[] args) {
    	HostDrawFrame h = new HostDrawFrame();
    }
}
