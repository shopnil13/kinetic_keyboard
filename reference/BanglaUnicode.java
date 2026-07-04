package net.hasnath.ridmikparser;
public class BanglaUnicode {
    private java.util.Map djkt;
    private java.util.Map djktt;
    private java.util.Map jkt;
    private java.util.Map kars;
    private java.util.Map map;

    public BanglaUnicode()
    {
        this.map = new java.util.HashMap();
        this.kars = new java.util.HashMap();
        this.jkt = new java.util.HashMap();
        this.djkt = new java.util.HashMap();
        this.djktt = new java.util.HashMap();
        this.map.put("o", "\u0985");
        this.map.put("O", "\u0993");
        this.map.put("a", "\u0986");
        this.map.put("A", "\u0986");
        this.map.put("S", "\u09b6");
        this.map.put("sh", "\u09b6");
        this.map.put("s", "\u09b8");
        this.map.put("Sh", "\u09b7");
        this.map.put("h", "\u09b9");
        this.map.put("H", "\u09b9");
        this.map.put("r", "\u09b0");
        this.map.put("R", "\u09dc");
        this.map.put("Rh", "\u09dd");
        this.map.put("k", "\u0995");
        this.map.put("K", "\u0995");
        this.map.put("q", "\u0995");
        this.map.put("qq", "\u0981");
        this.map.put("kh", "\u0996");
        this.map.put("g", "\u0997");
        this.map.put("G", "\u0997");
        this.map.put("gh", "\u0998");
        this.map.put("Ng", "\u0999");
        this.map.put("c", "\u099a");
        this.map.put("C", "\u099a");
        this.map.put("ch", "\u099b");
        this.map.put("j", "\u099c");
        this.map.put("jh", "\u099d");
        this.map.put("J", "\u099c");
        this.map.put("NG", "\u099e");
        this.map.put("T", "\u099f");
        this.map.put("Th", "\u09a0");
        this.map.put("TH", "\u09ce");
        this.map.put("f", "\u09ab");
        this.map.put("F", "\u09ab");
        this.map.put("ph", "\u09ab");
        this.map.put("i", "\u0987");
        this.map.put("I", "\u0988");
        this.map.put("e", "\u098f");
        this.map.put("E", "\u098f");
        this.map.put("u", "\u0989");
        this.map.put("U", "\u098a");
        this.map.put("b", "\u09ac");
        this.map.put("B", "\u09ac");
        this.map.put("w", "\u09ac");
        this.map.put("bh", "\u09ad");
        this.map.put("V", "\u09ad");
        this.map.put("v", "\u09ad");
        this.map.put("t", "\u09a4");
        this.map.put("th", "\u09a5");
        this.map.put("d", "\u09a6");
        this.map.put("dh", "\u09a7");
        this.map.put("D", "\u09a1");
        this.map.put("Dh", "\u09a2");
        this.map.put("n", "\u09a8");
        this.map.put("N", "\u09a3");
        this.map.put("z", "\u09af");
        this.map.put("Z", "\u09af");
        this.map.put("y", "\u09df");
        this.map.put("l", "\u09b2");
        this.map.put("L", "\u09b2");
        this.map.put("m", "\u09ae");
        this.map.put("M", "\u09ae");
        this.map.put("P", "\u09aa");
        this.map.put("p", "\u09aa");
        this.map.put("ng", "\u0982");
        this.map.put("cb", "\u0981");
        this.map.put("x", "\u0995\u09cd\u09b8");
        this.map.put("OU", "\u0994");
        this.map.put("OI", "\u0990");
        this.map.put("hs", "\u09cd");
        this.map.put("nj", "\u099e\u09cd\u099c");
        this.map.put("nc", "\u099e\u09cd\u099a");
        this.map.put("gg", "\u099c\u09cd\u099e");
        this.kars.put("o", "");
        this.kars.put("a", "\u09be");
        this.kars.put("A", "\u09be");
        this.kars.put("e", "\u09c7");
        this.kars.put("E", "\u09c7");
        this.kars.put("O", "\u09cb");
        this.kars.put("OI", "\u09c8");
        this.kars.put("OU", "\u09cc");
        this.kars.put("i", "\u09bf");
        this.kars.put("I", "\u09c0");
        this.kars.put("u", "\u09c1");
        this.kars.put("U", "\u09c2");
        this.kars.put("oo", "\u09c1");
        this.jkt.put("k", "kTtnNslw");
        this.jkt.put("g", "gnNmlw");
        this.jkt.put("ch", "w");
        this.jkt.put("Ng", "gkm");
        this.jkt.put("NG", "cj");
        this.jkt.put("g", "gnNmlw");
        this.jkt.put("G", "gnNmlw");
        this.jkt.put("th", "w");
        this.jkt.put("gh", "Nn");
        this.jkt.put("c", "c");
        this.jkt.put("j", "jw");
        this.jkt.put("T", "T");
        this.jkt.put("D", "D");
        this.jkt.put("R", "g");
        this.jkt.put("N", "DNmwT");
        this.jkt.put("t", "tnmwN");
        this.jkt.put("d", "wdmv");
        this.jkt.put("dh", "wn");
        this.jkt.put("n", "ndwmtsDT");
        this.jkt.put("p", "plTtns");
        this.jkt.put("f", "l");
        this.jkt.put("ph", "l");
        this.jkt.put("b", "jdbwl");
        this.jkt.put("v", "l");
        this.jkt.put("bh", "l");
        this.jkt.put("m", "npfwvmlb");
        this.jkt.put("l", "lwmpkgTDf");
        this.jkt.put("Sh", "kTNpmf");
        this.jkt.put("S", "clwnm");
        this.jkt.put("sh", "clwnm");
        this.jkt.put("s", "kTtnpfmlw");
        this.jkt.put("h", "Nnmlw");
        this.jkt.put("cb", "");
        this.jkt.put("jh", "");
        this.jkt.put("TH", "");
        this.jkt.put("qq", "");
        this.jkt.put("ng", "");
        this.jkt.put("kh", "");
        this.jkt.put("gg", "");
        this.jkt.put("dh", "");
        this.jkt.put("Th", "");
        this.djkt.put("kh", "Ngs");
        this.djkt.put("ch", "c");
        this.djkt.put("Dh", "N");
        this.djkt.put("ph", "mls");
        this.djkt.put("dh", "gdnbl");
        this.djkt.put("bh", "dm");
        this.djkt.put("Sh", "k");
        this.djkt.put("th", "tns");
        this.djkt.put("Th", "Nn");
        this.djkt.put("jh", "j");
        this.djkt.put("NG", "cj");
        this.djktt.put("ch", "NG");
        this.djktt.put("gh", "Ng");
        this.djktt.put("Th", "Sh");
        this.djktt.put("jh", "NG");
        this.djktt.put("sh", "ch");
        return;
    }

    public static void main(String[] p1)
    {
        String[] v0_1 = new String[0];
        net.hasnath.ridmikparser.RidmikParser.main(v0_1);
        return;
    }

    public String get(char p3)
    {
        return ((String) this.map.get(Character.toString(p3)));
    }

    public String getDjkt(char p4, char p5)
    {
        return ((String) this.djkt.get(new StringBuilder(String.valueOf(Character.toString(p4))).append(Character.toString(p5)).toString()));
    }

    public String getDjktt(char p4, char p5)
    {
        return ((String) this.djktt.get(new StringBuilder(String.valueOf(Character.toString(p4))).append(Character.toString(p5)).toString()));
    }

    public String getDual(char p4, char p5)
    {
        return ((String) this.map.get(new StringBuilder(String.valueOf(Character.toString(p5))).append(Character.toString(p4)).toString()));
    }

    public String getDualJkt(char p4, char p5)
    {
        return ((String) this.jkt.get(new StringBuilder(String.valueOf(Character.toString(p4))).append(Character.toString(p5)).toString()));
    }

    public String getDualKar(char p4, char p5)
    {
        return ((String) this.kars.get(new StringBuilder(String.valueOf(Character.toString(p5))).append(Character.toString(p4)).toString()));
    }

    public String getJkt(char p3)
    {
        return ((String) this.jkt.get(Character.toString(p3)));
    }

    public String getKar(char p3)
    {
        return ((String) this.kars.get(Character.toString(p3)));
    }
}
