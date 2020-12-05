package org.jp.zip;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.tree.*;

public class ZipTool extends JFrame implements MouseListener {

    String windowTitle = "ZipTool";
    JScrollPane jsp;
    Header header;
    Footer footer;
	Color bgColor = new Color(0xc6d8f9);
	JFileChooser openChooser = null;
	JFileChooser saveChooser = null;
	JPanel mainPanel;
	File currentFile;
	JTree currentTree;
	ColorPane cp;
	File temp = null;

    public static void main(String args[]) {
        new ZipTool();
    }

    public ZipTool() {
		super();

		setTitle(windowTitle);
		setBackground(bgColor);
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(bgColor);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		cp = new ColorPane();
		
		jsp = new JScrollPane();
		jsp.getViewport().setBackground(Color.white);
		mainPanel.add( jsp, BorderLayout.CENTER );
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		header = new Header();
		mainPanel.add(header, BorderLayout.NORTH);
		footer = new Footer();
		mainPanel.add(footer, BorderLayout.SOUTH);
		
		addWindowListener(new WindowCloser(this));

		pack();
		positionFrame();
		setVisible(true);

		new DropTarget(this, new FileDropTargetListener());
		new DropTarget(cp, new FileDropTargetListener());
		
		//make a temp directory
		try {
			temp = File.createTempFile("ZipTool-","");
			temp.delete();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			temp = new File("temp");
		}
		temp.mkdirs();
	}
	
	class Header extends JPanel implements ActionListener {
		JButton open;
		JButton extract;
		JButton list;
		JButton tree;
		public Header()  {
			super();
			setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
			setBackground(bgColor);
			Border inner = BorderFactory.createEmptyBorder(3, 3, 3, 3);
			Border outer = BorderFactory.createBevelBorder(BevelBorder.RAISED);
			setBorder(BorderFactory.createCompoundBorder(outer, inner));
			open = new JButton("Open");
			open.addActionListener(this);
			extract = new JButton("Extract");
			extract.addActionListener(this);
			list = new JButton("List");
			list.addActionListener(this);
			tree = new JButton("Tree");
			tree.addActionListener(this);
			add(open);
			add(Box.createHorizontalStrut(5));
			add(extract);
			add(Box.createHorizontalGlue());
			add(list);
			add(Box.createHorizontalStrut(5));
			add(tree);
		}
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			if (source.equals(open)) {
				open();
			}
			else if (source.equals(extract)) {
				if (currentTree != null) {
					DefaultMutableTreeNode node = 
							(DefaultMutableTreeNode) currentTree.getLastSelectedPathComponent();
					if (node == null) return;
					Object nodeInfo = node.getUserObject();
					if (nodeInfo instanceof ZipItem) {
						ZipItem z = (ZipItem)nodeInfo;
						System.out.println(z.path);
						File destination = getDestination();
						if (destination != null) extract(node, z.parent.length(), destination);
					}
				}
			}
			else if (source.equals(list)) {
				if (currentTree != null) {
					jsp.setViewportView(cp);
				}
			}
			else if (source.equals(tree)) {
				if (currentTree != null) {
					jsp.setViewportView(currentTree);
				}				
			}
		}
	}
	
	class Footer extends JPanel {
		JLabel msg;
		public Footer()  {
			super();
			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			setBackground(bgColor);
			Border inner = BorderFactory.createEmptyBorder(5, 5, 5, 5);
			Border outer = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
			setBorder(BorderFactory.createCompoundBorder(outer, inner));
			msg = new JLabel(" ");
			add(Box.createHorizontalStrut(5));
			add(msg);
		}
		public void setText(String text) {
			msg.setText(text);
		}
	}
	
    class FileDropTargetListener implements DropTargetListener {
		public FileDropTargetListener() {
			super();
		}
		public void dragEnter(DropTargetDragEvent dtde) { }
		public void dragExit(DropTargetEvent dte) { }
		public void dragOver(DropTargetDragEvent dtde) { }
		public void dropActionChanged(DropTargetDragEvent dtde) { }
		public void drop(DropTargetDropEvent dtde) {
			dtde.acceptDrop(DnDConstants.ACTION_COPY);
			Transferable transferable = dtde.getTransferable();
			try {
				@SuppressWarnings("unchecked")
				// getTransferData(DataFlavor.javaFileListFlavor) is guaranteed to return a File List.
				java.util.List<File> files = 
						(java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				if (!files.isEmpty()) {
					File file = files.get(0);
					try {
						//Make sure it parses
						ZipFile zf = new ZipFile(file); 
						openFile(file);
					}
					catch (Exception ignore) { }
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			finally {
				dtde.dropComplete(true);
			}
		}		
	}

	public void open() {
		if (openChooser == null) {
			File here = new File(System.getProperty("user.dir"));
			openChooser = new JFileChooser(here);
			openChooser.setDialogTitle("Select a Zip File to open");
			openChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		}
		if (openChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = openChooser.getSelectedFile();
			if ((file != null) && file.exists()) openFile(file);
		}
	}
	
	public void openFile(File file) {
		try {
			emptyTemp();
			footer.setText(file.getAbsolutePath());
			ZipItem[] items = getZipItems(file);
			currentTree = createTree(file.getName(), items);
			currentTree.addMouseListener(this);
			setTreeExpandedState(currentTree, true);
			jsp.setViewportView(currentTree);
			cp.setEditable(true);
			cp.setText(getZipEntriesListing(file));
			cp.setEditable(false);
			currentFile = file;
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public File getDestination() {
		if (saveChooser == null) {
			File here = new File(System.getProperty("user.dir"));
			saveChooser = new JFileChooser(here);
			saveChooser.setDialogTitle("Select a directory in which to save the selection");
			saveChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			return saveChooser.getSelectedFile();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void extract(DefaultMutableTreeNode node, int rootlen, File dir) {
		Object nodeInfo = node.getUserObject();
		if (nodeInfo instanceof ZipItem) {
			ZipItem zi = (ZipItem)nodeInfo;
			if (zi.isLeaf) {
				String subPath = zi.path.substring(rootlen);
				File f = new File(dir, subPath);
				File p = f.getParentFile();
				p.mkdirs();
				getFile(zi, f);				
			}
			else {
				ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
				for (DefaultMutableTreeNode treeNode : list) {
					extract(treeNode, rootlen, dir);
				}
			}
		}
	}
	
    public void mouseClicked(MouseEvent e) {
		DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode) currentTree.getLastSelectedPathComponent();
		if (node == null) return;
		Object nodeInfo = node.getUserObject();
		if (nodeInfo instanceof ZipItem) {
			ZipItem z = (ZipItem)nodeInfo;
			if ((e.getClickCount() == 2) && z.isLeaf) {
				File file = getFile(z);
				if (file != null) launchFile(file);
			}
		}
	}
	public void mouseEntered(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	
	private void positionFrame() {
		Toolkit tk = getToolkit();
		Dimension scr = tk.getScreenSize ();
		setSize( 800, 700 );
		int x = (scr.width - getSize().width)/2;
		int y = (scr.height - getSize().height)/2;
		setLocation( new Point(x,y) );
	}

    class WindowCloser extends WindowAdapter {
		public WindowCloser(JFrame parent) { }
		public void windowClosing(WindowEvent evt) {
			deleteAll(temp);
			System.exit(0);
		}
    }
    
	private void deleteAll(File file) {
		if ((file != null) && file.exists()) {
			if (file.isDirectory()) {
				try {
					File[] files = file.listFiles();
					for (File f : files) deleteAll(f);
				}
				catch (Exception e) { return; }
			}
			file.delete();
		}
	}

    private ZipItem[] getZipItems(File file) throws Exception {
		if (!file.exists()) throw new Exception("Zip file does not exist ("+file+")");
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		LinkedList<ZipItem> list = new LinkedList<ZipItem>();
		while (zipEntries.hasMoreElements()) {
			ZipEntry ze = zipEntries.nextElement();
			ZipItem item = new ZipItem(ze);
			list.add(item);
			//System.out.println(item.path);
		}
		zipFile.close();
		ZipItem[] items = list.toArray(new ZipItem[list.size()]);
		Arrays.sort(items);
		return items;
	}
	
	private String getZipEntriesListing(File file) {
		StringBuffer sb = new StringBuffer(file.getName() + "\n");
		try {
			ZipFile zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry ze = zipEntries.nextElement();
				sb.append(ze.getName().replace('\\','/') + "\n");
			}
			zipFile.close();
		}
		catch (Exception ex) { }
		return sb.toString();
	}
	
	private JTree createTree(String name, ZipItem[] items) {
		ZipItem programItem = new ZipItem(name, "");
		Hashtable<String,DefaultMutableTreeNode> parents = new Hashtable<String,DefaultMutableTreeNode>();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(programItem);
		parents.put("", root);
		JTree tree = new JTree(root);
		TreeSelectionModel tsm = tree.getSelectionModel();
		tsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setSelectionModel(tsm);
		tree.setDragEnabled(true);
		tree.setTransferHandler(new FileTransferHandler());
		new DropTarget(tree, new FileDropTargetListener());

		for (ZipItem z : items) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(z);
			DefaultMutableTreeNode parent = parents.get(z.parent);
			if (parent == null) {
				String[] ps = z.parent.split("/");
				String p = "";
				for (int i=0; i<ps.length; i++) {
					p += ps[i] + "/";
					if (!parents.containsKey(p)) {
						ZipItem zi = new ZipItem(p);
						parent = new DefaultMutableTreeNode(zi);
						parents.get(zi.parent).add(parent);
						parents.put(zi.path, parent);
					}
				}
			}
			parent.add(node);
			if (!z.isLeaf) parents.put(z.path, node);
		}
		return tree;
	}

	private void setTreeExpandedState(JTree tree, boolean expanded) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
		setNodeExpandedState(tree, node, expanded);
	}

	@SuppressWarnings("unchecked")
	private void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node, boolean expanded) {
		ArrayList<DefaultMutableTreeNode> list = Collections.list(node.children());
		for (DefaultMutableTreeNode treeNode : list) {
			setNodeExpandedState(tree, treeNode, expanded);
		}
		if (!expanded && node.isRoot()) return;
		TreePath path = new TreePath(node.getPath());
		if (expanded) tree.expandPath(path);
		else tree.collapsePath(path);
	}
	
	private void launchFile(File file) {
		try { Desktop.getDesktop().open(file); }
		catch (Exception ex) { ex.printStackTrace(); }
	}
	
	private void emptyTemp() {
		for (File f : temp.listFiles()) {
			f.delete();
		}
	}
	
	private File getFile(ZipItem z) {
		File outFile = new File(temp, z.name);
		return getFile(z, outFile);
	}
	
	private File getFile(ZipItem z, File outFile) {
		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		ZipFile zipFile = null;
		if (!currentFile.exists()) return null;
		try {
			zipFile = new ZipFile(currentFile);
			out = new BufferedOutputStream(new FileOutputStream(outFile));
			in = new BufferedInputStream(zipFile.getInputStream(z.zipEntry));
			if (!copy(in, out)) outFile = null;
		}
		catch (Exception e) { outFile = null; }
		finally {
			try {
				in.close();
				out.close();
				zipFile.close();
			}
			catch (Exception ignore) { }
		}
		return outFile;
	}
	
	private boolean copy(InputStream in, OutputStream out) {
		boolean result = true;
		try {
			int bufferSize = 1024 * 64;
			BufferedInputStream bin = new BufferedInputStream(in);
			byte[] b = new byte[bufferSize];
			int n;
			while ((n = bin.read(b, 0, b.length)) != -1) out.write(b, 0, n);
			out.flush();
		}
		catch (Exception ex) { result = false; }
		finally {
			try { 
				in.close();
				out.close();
			}
			catch (Exception ignore) { }
		}
		return result;
	}

	private class FileTransferHandler extends TransferHandler {

		@Override
		protected Transferable createTransferable(JComponent c) {
			JTree tree = (JTree) c;
			DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			if (node == null) return null;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof ZipItem) {
				ZipItem z = (ZipItem)nodeInfo;
				if (!z.isLeaf) return null;
				File file = getFile(z);
				List<File> files = new ArrayList<File>();
				files.add(file);
				return new FileTransferable(files);
			}
			return null;
		}

		@Override
		public int getSourceActions(JComponent c) {
			return COPY;
		}
	}

	private class FileTransferable implements Transferable {

		private List<File> files;

		public FileTransferable(List<File> files) {
			this.files = files;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{DataFlavor.javaFileListFlavor};
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(DataFlavor.javaFileListFlavor);
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return files;
		}
	}

}
