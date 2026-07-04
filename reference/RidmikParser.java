package net.hasnath.ridmikparser;
public class RidmikParser {
    private net.hasnath.ridmikparser.BanglaUnicode unicode;

    public RidmikParser()
    {
        this.unicode = new net.hasnath.ridmikparser.BanglaUnicode();
        return;
    }

    public static void main(String[] p5)
    {
        net.hasnath.ridmikparser.RidmikParser v1_1 = new net.hasnath.ridmikparser.RidmikParser();
        java.util.Scanner v2_1 = new java.util.Scanner(System.in);
        System.out.println("Enter:");
        while (v2_1.hasNext()) {
            System.out.println(v1_1.toBangla(v2_1.nextLine()));
        }
        return;
    }

    boolean dualSitsUnder(char p6, char p7, char p8, char p9)
    {
        String v2_0 = 1;
        if ((p7 != 114) || (p6 != 114)) {
            if (p7 != 114) {
                String v0 = this.unicode.getDjkt(p8, p9);
                if ((v0 == null) || (!this.isCharInString(p7, v0))) {
                    String v1 = this.unicode.getDjktt(p8, p9);
                    if (v1 == null) {
                        v2_0 = 0;
                    } else {
                        v2_0 = v1.contains(new StringBuilder(String.valueOf(Character.toString(p6))).append(Character.toString(p7)).toString());
                    }
                }
            } else {
                v2_0 = 0;
            }
        }
        return v2_0;
    }

    boolean isCharInString(char p3, String p4)
    {
        int v0_1;
        if (p4.indexOf(p3) != -1) {
            v0_1 = 1;
        } else {
            v0_1 = 0;
        }
        return v0_1;
    }

    boolean isConsonant(char p2)
    {
        if ((this.isVowel(p2)) || (!Character.isLetter(p2))) {
            int v0_2 = 0;
        } else {
            v0_2 = 1;
        }
        return v0_2;
    }

    boolean isVowel(char p3)
    {
        int v0_2;
        if ("AEIOUaeiou".indexOf(p3) != -1) {
            v0_2 = 1;
        } else {
            v0_2 = 0;
        }
        return v0_2;
    }

    boolean notJukta(char p6, char p7, char p8, char p9)
    {
        int v1 = 0;
        if ((p7 != 110) || ((p8 != 103) || (p9 != 114))) {
            if ((p9 != 114) && ((p9 != 122) && (p9 != 119))) {
                String v0_0 = this.unicode.getDualJkt(p7, p8);
                if (v0_0 == null) {
                    String v0_1 = this.unicode.getJkt(p8);
                    if (v0_1 == null) {
                        v1 = 1;
                    } else {
                        if (!this.isCharInString(p9, v0_1)) {
                            v1 = 1;
                        }
                    }
                } else {
                    if (!this.isCharInString(p9, v0_0)) {
                        v1 = 1;
                    }
                }
            }
        } else {
            v1 = 1;
        }
        return v1;
    }

    public String toBangla(String p19)
    {
        StringBuilder v10_1 = new StringBuilder();
        int v1 = 0;
        int v9 = 0;
        int v12 = 0;
        int v5 = 0;
        int v8 = 0;
        int v7 = 0;
        char[] v2 = p19.toCharArray();
        int v14 = v2.length;
        int v13_0 = 0;
        while (v13_0 < v14) {
            int v6 = v2[v13_0];
            if (((v6 >= 97) && (v6 <= 122)) || (((v6 >= 65) && (v6 <= 90)) || ((v6 >= 48) && (v6 <= 57)))) {
                if ((v6 == 65) || ((v6 == 66) || ((v6 == 67) || ((v6 == 69) || ((v6 == 70) || ((v6 == 80) || (v6 == 88))))))) {
                    v6 = Character.toLowerCase(v6);
                }
                if ((v6 == 75) || ((v6 == 76) || ((v6 == 77) || ((v6 == 86) || ((v6 == 89) || ((v6 == 87) || (v6 == 81))))))) {
                    v6 = Character.toLowerCase(v6);
                }
                if ((v6 == 72) && (v1 != 84)) {
                    v6 = 104;
                }
                if (((v1 == 0) || (this.isVowel(v1))) && (v6 == 119)) {
                    v6 = 79;
                }
                if (this.isVowel(v6)) {
                    if ((v1 != 114) || ((v9 != 114) || (v6 != 105))) {
                        String v3_1;
                        if (v9 == 0) {
                            v3_1 = this.unicode.getDual(v6, v1);
                        } else {
                            v3_1 = this.unicode.getDualKar(v6, v1);
                        }
                        if (v3_1 == null) {
                            if ((v6 != 111) || (v1 == 0)) {
                                if ((!this.isVowel(v1)) && (v1 != 0)) {
                                    v10_1.append(this.unicode.getKar(v6));
                                } else {
                                    if ((v6 != 97) || (v1 == 0)) {
                                        v10_1.append(this.unicode.get(v6));
                                    } else {
                                        v10_1.append(this.unicode.get(121)).append(this.unicode.getKar(97));
                                    }
                                }
                            } else {
                                if (!this.isVowel(v1)) {
                                    v12 = v9;
                                    v9 = v1;
                                    v1 = v6;
                                    v13_0++;
                                } else {
                                    v10_1.append(this.unicode.get(79));
                                }
                            }
                        } else {
                            if (v1 != 111) {
                                v10_1.delete((v10_1.length() - 1), v10_1.length());
                            }
                            if (!this.isVowel(v9)) {
                                v10_1.append(v3_1);
                            } else {
                                v10_1.append(this.unicode.get(v1)).append(this.unicode.get(v6));
                            }
                        }
                    } else {
                        if (v12 != 0) {
                            v10_1.delete((v10_1.length() - 3), v10_1.length());
                            v10_1.append("\u09c3");
                        } else {
                            v10_1.delete((v10_1.length() - 2), v10_1.length());
                            v10_1.append("\u098b");
                        }
                        v1 = 105;
                    }
                }
                if ((v6 == 121) || ((v6 == 90) || (v6 == 114))) {
                    v5 = 0;
                }
                if ((v5 == 0) || (this.unicode.getDual(v6, v1) != null)) {
                    int v11 = 0;
                } else {
                    v11 = 1;
                }
                if ((!this.isConsonant(v6)) || ((!this.isConsonant(v1)) || (v11 != 0))) {
                    if (this.isConsonant(v6)) {
                        v7 = 0;
                        if ((this.isVowel(v1)) && (v6 == 90)) {
                            v10_1.append("\u09cd");
                        }
                        if ((v1 == 0) && (v6 == 120)) {
                            v10_1.append(this.unicode.get(101));
                        }
                        v8 = v5;
                        v5 = 0;
                        if ((v6 == 119) && ((this.isConsonant(v1)) && (this.isConsonant(v9)))) {
                            v10_1.append("\u09cd");
                            v8 = 0;
                            v5 = 1;
                        }
                        if ((v12 == 107) && ((v9 == 83) && ((v1 == 104) && ((v6 == 78) || (v6 == 109))))) {
                            v10_1.append("\u09cd");
                            v8 = 0;
                            v5 = 1;
                        }
                        v10_1.append(this.unicode.get(v6));
                    }
                } else {
                    if (((v6 == 121) || (v6 == 90)) && ((v6 != 121) || ((v1 != 113) || (v9 != 113)))) {
                        v6 = 122;
                    }
                    if ((v9 == 107) && ((v1 == 107) && (v6 == 104))) {
                        v1 = 83;
                    }
                    String v3_0 = this.unicode.getDual(v6, v1);
                    if ((v3_0 == null) || (v7 != 0)) {
                        v7 = 0;
                        v8 = v5;
                        v5 = 0;
                        if ((v9 == 114) || ((v1 != 114) || (v6 != 122))) {
                            if (((v1 != 114) || (v9 == 114)) && ((v1 != 114) || ((v9 != 114) || (!this.isConsonant(v12))))) {
                                if ((v1 != 114) || ((v9 != 114) || ((!this.isVowel(v12)) && (v12 != 0)))) {
                                    if (!this.notJukta(v12, v9, v1, v6)) {
                                        v10_1.append("\u09cd");
                                        v5 = 1;
                                    }
                                } else {
                                    v10_1.delete((v10_1.length() - 1), v10_1.length());
                                    v10_1.append("\u09cd");
                                }
                            }
                        } else {
                            v10_1.append("\u200d\u09cd");
                        }
                        v10_1.append(this.unicode.get(v6));
                    } else {
                        v7 = 1;
                        if ((v12 == 103) && ((v9 == 107) && ((v1 == 83) && (v6 == 104)))) {
                            v5 = 0;
                            v8 = 0;
                        }
                        if ((this.isVowel(v9)) || ((v9 == 0) || (v8 != 0))) {
                            int v4 = 1;
                        } else {
                            v4 = 0;
                        }
                        if ((!this.dualSitsUnder(v12, v9, v1, v6)) || (v4 != 0)) {
                            if (v5 == 0) {
                                v10_1.delete((v10_1.length() - 1), v10_1.length());
                            } else {
                                v10_1.delete((v10_1.length() - 2), v10_1.length());
                            }
                            v10_1.append(v3_0);
                            v8 = v5;
                            v5 = 0;
                        } else {
                            v10_1.delete((v10_1.length() - 1), v10_1.length());
                            if ((v9 == 114) && (v12 == 114)) {
                                v10_1.delete((v10_1.length() - 1), v10_1.length());
                            }
                            if ((v5 == 0) && ((v9 != 0) && (!this.isVowel(v9)))) {
                                v10_1.append("\u09cd");
                            }
                            v10_1.append(v3_0);
                            v8 = v5;
                            v5 = 1;
                        }
                    }
                }
                v12 = v9;
                v9 = v1;
                v1 = v6;
            } else {
                v10_1.append(v6);
                v1 = 0;
            }
        }
        return v10_1.toString();
    }
}
