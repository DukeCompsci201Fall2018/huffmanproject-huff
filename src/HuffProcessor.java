import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
		
		
	}
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while (true){
			Integer val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			String code = codings[val];
			 out.writeBits(code.length(), Integer.parseInt(code,2));
		}
		String code = codings[PSEUDO_EOF];
	    out.writeBits(code.length(), Integer.parseInt(code,2));


		
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if(out.bitsWritten()==0) {
			out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		}
		if (root.myLeft!=null || root.myRight!=null) {
			out.writeBits(1,0);
		    writeHeader(root.myLeft, out);
		    writeHeader(root.myRight, out);
		}
		else {
		    int value = root.myValue;
		    out.writeBits(1,1);
		    out.writeBits(BITS_PER_WORD+1,value);
		}
	}
		
	

	private String[] makeCodingsFromTree(HuffNode root) {
	    String[] encodings = new String[ALPH_SIZE + 1];
	    codingHelper(root,"",encodings);

		return encodings;
	}

	private void codingHelper(HuffNode root, String string, String[] encodings) {
			if (root.myLeft==null && root.myRight== null) {
		        encodings[root.myValue] = string;
		        return;
		   }
		   codingHelper(root.myLeft, string+"0", encodings);
		   codingHelper(root.myRight, string+"1", encodings);
		
	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int t = 0; t<counts.length; t++) {
			if (counts[t]>0) {
				pq.add(new HuffNode(t,counts[t],null,null));
			}
		    
		}

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    HuffNode s= new HuffNode(-1,left.myWeight+right.myWeight,left,right);
		    pq.add(s);
		}

		return pq.remove();
	}

	private int[] readForCounts(BitInputStream in) {
		int [] counts = new int [ALPH_SIZE + 1];
		System.out.println(" blank");
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) {
				break;
			}
			System.out.println(val + " random");
			counts[val]+= 1;
		}
		counts[PSEUDO_EOF] = 1;
		return counts;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with"+ bits);
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits (root, in, out);
		out.close();
	}

	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		 HuffNode current = root; 
		   while (true) {
		       int bits = in.readBits(1);
		       if (bits == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else{
		    	   if (bits == 0) {
		    		   current = current.myLeft;
		    	   }
		    	   else if (bits ==1){
		        	   current = current.myRight;
		    	   }
		    	   if (current != null&&current.myLeft==null && current.myRight==null) {
			             if (current.myValue == PSEUDO_EOF) { 
			                   break;
			              }
			               else {
			            	   
			                   out.writeBits(BITS_PER_WORD, current.myValue);;
			                   current = root; // start back after leaf
			                  
			               }
			    	   }
		       }
		   }
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		HuffNode left;
		HuffNode right;
		int value;
		int bits = in.readBits(1);
		if (bits == -1) {
			throw new HuffException("reading bits failed");
		}

		if (bits == 0) {
		    left = readTreeHeader(in);
		    right = readTreeHeader(in);
		    return new HuffNode(0,0,left,right);
		}
		else {
		    value = in.readBits(BITS_PER_WORD+1);
		    return new HuffNode(value,0,null,null);
		}
	}
}