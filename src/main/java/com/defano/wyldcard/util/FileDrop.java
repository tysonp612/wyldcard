package com.defano.wyldcard.util;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/**
 * This class makes it easy to drag and drop files from the operating
 * system to a Java program. Any <tt>java.awt.Component</tt> can be
 * dropped onto, but only <tt>javax.swing.JComponent</tt>s will indicate
 * the drop event with a changed border.
 * <p/>
 * To use this class, construct a new <tt>FileDrop</tt> by passing
 * it the target component and a <tt>Listener</tt> to receive notification
 * when file(s) have been dropped. Here is an example:
 * <p/>
 * <code><pre>
 *      JPanel myPanel = new JPanel();
 *      new FileDrop( myPanel, new FileDrop.Listener()
 *      {   public void filesDropped( java.io.File[] files )
 *          {
 *              // handle file drop
 *              ...
 *          }   // end filesDropped
 *      }); // end FileDrop.Listener
 * </pre></code>
 * <p/>
 * You can specify the border that will appear when files are being dragged by
 * calling the constructor with a <tt>javax.swing.border.Border</tt>. Only
 * <tt>JComponent</tt>s will show any indication with a border.
 * <p/>
 * You can turn on some debugging features by passing a <tt>PrintStream</tt>
 * object (such as <tt>System.out</tt>) into the full constructor. A <tt>null</tt>
 * value will result in no extra debugging information being output.
 * <p/>
 *
 * <p>I'm releasing this code into the Public Domain. Enjoy.
 * </p>
 * <p><em>Original author: Robert Harder, rharder@usa.net</em></p>
 * <p>2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.</p>
 *
 * @author  Robert Harder
 * @author  rharder@users.sf.net
 * @version 1.0.1
 */
public class FileDrop
{
    private static final String ZERO_CHAR_STRING = "" + (char)0;
    private transient javax.swing.border.Border normalBorder;
    private transient java.awt.dnd.DropTargetListener dropListener;


    /** Discover if the running JVM is modern enough to have drag and drop. */
    private static Boolean supportsDnD;

    // Default border color
    private static final java.awt.Color defaultBorderColor = new java.awt.Color( 0f, 0f, 1f, 0.25f );

    public FileDrop(final java.awt.Component c, final Listener listener) {
        this(null, c, javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), true, listener);
    }

    public FileDrop(final java.awt.Component c, final boolean recursive, final Listener listener) {
        this(null, c, javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), recursive, listener);
    }

    public FileDrop(final java.io.PrintStream out, final java.awt.Component c, final Listener listener) {
        this(out, c, javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), false, listener);
    }

    public FileDrop(final java.awt.Component c, final javax.swing.border.Border dragBorder, final Listener listener) {
        this(null, c, dragBorder, false, listener);
    }

    public FileDrop(final java.awt.Component c, final javax.swing.border.Border dragBorder, final boolean recursive, final Listener listener) {
        this(null, c, dragBorder, recursive, listener);
    }

    public FileDrop(final java.io.PrintStream out, final java.awt.Component c, final javax.swing.border.Border dragBorder, final Listener listener) {
        this(out, c, dragBorder, false, listener);
    }

    public FileDrop(final java.io.PrintStream out, final java.awt.Component c, final javax.swing.border.Border dragBorder, final boolean recursive, final Listener listener) {
        if (supportsDnD()) {
            dropListener = createDropTargetListener(out, c, dragBorder, listener);
            makeDropTarget(out, c, recursive);
        } else {
            log(out, "FileDrop: Drag and drop is not supported with this JVM");
        }
    }

    private java.awt.dnd.DropTargetListener createDropTargetListener(final java.io.PrintStream out, final java.awt.Component c, final javax.swing.border.Border dragBorder, final Listener listener) {
        return new java.awt.dnd.DropTargetListener() {
            public void dragEnter(java.awt.dnd.DropTargetDragEvent evt) {
                handleDragEnter(out, c, dragBorder, evt);
            }

            public void dragOver(java.awt.dnd.DropTargetDragEvent evt) {}

            public void drop(java.awt.dnd.DropTargetDropEvent evt) {
                handleDrop(out, c, listener, evt);
            }

            public void dragExit(java.awt.dnd.DropTargetEvent evt) {
                handleDragExit(out, c);
            }

            public void dropActionChanged(java.awt.dnd.DropTargetDragEvent evt) {
                handleDropActionChanged(out, evt);
            }
        };
    }

    private void handleDragEnter(final java.io.PrintStream out, final java.awt.Component c, final javax.swing.border.Border dragBorder, java.awt.dnd.DropTargetDragEvent evt) {
        log(out, "FileDrop: dragEnter event.");
        if (isDragOk(out, evt)) {
            if (c instanceof javax.swing.JComponent) {
                javax.swing.JComponent jc = (javax.swing.JComponent) c;
                normalBorder = jc.getBorder();
                log(out, "FileDrop: normal border saved.");
                jc.setBorder(dragBorder);
                log(out, "FileDrop: drag border set.");
            }
            evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
            log(out, "FileDrop: event accepted.");
        } else {
            evt.rejectDrag();
            log(out, "FileDrop: event rejected.");
        }
    }

    private void handleDrop(final java.io.PrintStream out, final java.awt.Component c, final Listener listener, java.awt.dnd.DropTargetDropEvent evt) {
        log(out, "FileDrop: drop event.");
        try {
            java.awt.datatransfer.Transferable tr = evt.getTransferable();
            if (tr.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                handleFileListDrop(out, listener, evt, tr);
            } else {
                handleReaderDrop(out, listener, evt, tr);
            }
        } catch (IOException | java.awt.datatransfer.UnsupportedFlavorException e) {
            log(out, "FileDrop: Exception - abort:");
            e.printStackTrace(out);
            evt.rejectDrop();
        } finally {
            if (c instanceof javax.swing.JComponent) {
                javax.swing.JComponent jc = (javax.swing.JComponent) c;
                jc.setBorder(normalBorder);
                log(out, "FileDrop: normal border restored.");
            }
        }
    }

    private void handleFileListDrop(final java.io.PrintStream out, final Listener listener, java.awt.dnd.DropTargetDropEvent evt, java.awt.datatransfer.Transferable tr) throws IOException, java.awt.datatransfer.UnsupportedFlavorException {
        evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
        log(out, "FileDrop: file list accepted.");
        java.util.List fileList = (java.util.List) tr.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
        java.io.File[] files = (java.io.File[]) fileList.toArray(new java.io.File[fileList.size()]);
        if (listener != null) listener.filesDropped(files);
        evt.getDropTargetContext().dropComplete(true);
        log(out, "FileDrop: drop complete.");
    }

    private void handleReaderDrop(final java.io.PrintStream out, final Listener listener, java.awt.dnd.DropTargetDropEvent evt, java.awt.datatransfer.Transferable tr) throws IOException, java.awt.datatransfer.UnsupportedFlavorException {
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        boolean handled = false;
        for (DataFlavor flavor : flavors) {
            if (flavor.isRepresentationClassReader()) {
                evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                log(out, "FileDrop: reader accepted.");
                Reader reader = flavor.getReaderForText(tr);
                BufferedReader br = new BufferedReader(reader);
                if (listener != null) listener.filesDropped(createFileArray(br, out));
                evt.getDropTargetContext().dropComplete(true);
                log(out, "FileDrop: drop complete.");
                handled = true;
                break;
            }
        }
        if (!handled) {
            log(out, "FileDrop: not a file list or reader - abort.");
            evt.rejectDrop();
        }
    }

    private void handleDragExit(final java.io.PrintStream out, final java.awt.Component c) {
        log(out, "FileDrop: dragExit event.");
        if (c instanceof javax.swing.JComponent) {
            javax.swing.JComponent jc = (javax.swing.JComponent) c;
            jc.setBorder(normalBorder);
            log(out, "FileDrop: normal border restored.");
        }
    }

    private void handleDropActionChanged(final java.io.PrintStream out, java.awt.dnd.DropTargetDragEvent evt) {
        log(out, "FileDrop: dropActionChanged event.");
        if (isDragOk(out, evt)) {
            evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
            log(out, "FileDrop: event accepted.");
        } else {
            evt.rejectDrag();
            log(out, "FileDrop: event rejected.");
        }
    }

    private static boolean supportsDnD() {
        if (supportsDnD == null) {
            try {
                Class.forName("java.awt.dnd.DnDConstants");
                supportsDnD = true;
            } catch (Exception e) {
                supportsDnD = false;
            }
        }
        return supportsDnD;
    }

    private static File[] createFileArray(BufferedReader bReader, PrintStream out) {
        java.util.List list = new java.util.ArrayList();
        String line;

        try {
            while ((line = bReader.readLine()) != null) {
                try {
                    if (!line.equals(ZERO_CHAR_STRING)) {
                        java.io.File file = new java.io.File(new java.net.URI(line));
                        list.add(file);
                    }
                } catch (Exception ex) {
                    log(out, "Error with " + line + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            log(out, "FileDrop: IOException");
        }
        return (java.io.File[]) list.toArray(new File[list.size()]);
    }

    private void makeDropTarget(final java.io.PrintStream out, final java.awt.Component c, boolean recursive) {
        final java.awt.dnd.DropTarget dt = new java.awt.dnd.DropTarget();
        try {
            dt.addDropTargetListener(dropListener);
        } catch (java.util.TooManyListenersException e) {
            e.printStackTrace();
            log(out, "FileDrop: Drop will not work due to previous error. Do you have another listener attached?");
        }
        c.addHierarchyListener(evt -> {
            log(out, "FileDrop: Hierarchy changed.");
            if (c.getParent() == null) {
                c.setDropTarget(null);
                log(out, "FileDrop: Drop target cleared from component.");
            } else {
                new java.awt.dnd.DropTarget(c, dropListener);
                log(out, "FileDrop: Drop target added to component.");
            }
        });
        if (c.getParent() != null) new java.awt.dnd.DropTarget(c, dropListener);
        if (recursive && c instanceof java.awt.Container) {
            java.awt.Component[] comps = ((java.awt.Container) c).getComponents();
            for (java.awt.Component comp : comps) makeDropTarget(out, comp, recursive);
        }
    }

    private boolean isDragOk(final java.io.PrintStream out, final java.awt.dnd.DropTargetDragEvent evt) {
        boolean ok = false;
        java.awt.datatransfer.DataFlavor[] flavors = evt.getCurrentDataFlavors();
        for (java.awt.datatransfer.DataFlavor curFlavor : flavors) {
            if (curFlavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor) || curFlavor.isRepresentationClassReader()) {
                ok = true;
                break;
            }
        }
        if (out != null) {
            if (flavors.length == 0) log(out, "FileDrop: no data flavors.");
            for (java.awt.datatransfer.DataFlavor flavor : flavors) log(out, flavor.toString());
        }
        return ok;
    }

    private static void log(java.io.PrintStream out, String message) {
        if (out != null) out.println(message);
    }

    public static boolean remove(java.awt.Component c) {
        return remove(null, c, true);
    }

    public static boolean remove(java.io.PrintStream out, java.awt.Component c, boolean recursive) {
        if (supportsDnD()) {
            log(out, "FileDrop: Removing drag-and-drop hooks.");
            c.setDropTarget(null);
            if (recursive && (c instanceof java.awt.Container)) {
                java.awt.Component[] comps = ((java.awt.Container) c).getComponents();
                for (java.awt.Component comp : comps) remove(out, comp, recursive);
                return true;
            } else return false;
        } else return false;
    }

    public interface Listener {
        void filesDropped(java.io.File[] files);
    }

    public static class Event extends java.util.EventObject {
        private final java.io.File[] files;

        public Event(java.io.File[] files, Object source) {
            super(source);
            this.files = files;
        }

        public java.io.File[] getFiles() {
            return files;
        }
    }

    public static class TransferableObject implements java.awt.datatransfer.Transferable {
        public static final String MIME_TYPE = "application/x-net.iharder.dnd.TransferableObject";
        public static final java.awt.datatransfer.DataFlavor DATA_FLAVOR = new java.awt.datatransfer.DataFlavor(FileDrop.TransferableObject.class, MIME_TYPE);
        private Fetcher fetcher;
        private Object data;
        private java.awt.datatransfer.DataFlavor customFlavor;

        public TransferableObject(Object data) {
            this.data = data;
            this.customFlavor = new java.awt.datatransfer.DataFlavor(data.getClass(), MIME_TYPE);
        }

        public TransferableObject(Fetcher fetcher) {
            this.fetcher = fetcher;
        }

        public TransferableObject(Class dataClass, Fetcher fetcher) {
            this.fetcher = fetcher;
            this.customFlavor = new java.awt.datatransfer.DataFlavor(dataClass, MIME_TYPE);
        }

        public java.awt.datatransfer.DataFlavor getCustomDataFlavor() {
            return customFlavor;
        }

        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
            if (customFlavor != null)
                return new java.awt.datatransfer.DataFlavor[]{customFlavor, DATA_FLAVOR, java.awt.datatransfer.DataFlavor.stringFlavor};
            else
                return new java.awt.datatransfer.DataFlavor[]{DATA_FLAVOR, java.awt.datatransfer.DataFlavor.stringFlavor};
        }

        public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException {
            if (flavor.equals(DATA_FLAVOR)) return fetcher == null ? data : fetcher.getObject();
            if (flavor.equals(java.awt.datatransfer.DataFlavor.stringFlavor)) return fetcher == null ? data.toString() : fetcher.getObject().toString();
            throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
        }

        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
            if (flavor.equals(DATA_FLAVOR)) return true;
            if (flavor.equals(java.awt.datatransfer.DataFlavor.stringFlavor)) return true;
            return false;
        }

        public interface Fetcher {
            Object getObject();
        }
    }
}
