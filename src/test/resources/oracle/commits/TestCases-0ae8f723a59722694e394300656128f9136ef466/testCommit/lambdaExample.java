public class lambdaExample {
    public int read() throws IOException {
        return extracted(contains,
                (Integer i) -> {
                    String containsStr = (String) contains.elementAt(i);
                    return line.indexOf(containsStr) >= 0;
                });
    }

    public int read2() throws IOException {
        return extracted(regexps,
                (Integer i) -> {
                    RegularExpression regexp = (RegularExpression) regexps.elementAt(i);
                    Regexp re = regexp.getRegexp(getProject());
                    return re.matches(line);
                });
    }

    protected int extracted(Vector vector,
            Function<Integer, Boolean> matcher)
            throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }
        int ch = -1;
        if (line != null) {
            ch = line.charAt(0);
            if (line.length() == 1) {
                line = null;
            } else {
                line = line.substring(1);
            }
        } else {
            final int containsSize = vector.size();
            for (line = readLine(); line != null; line = readLine()) 
            {
                boolean matches = true;
                for (int i = 0; matches && i < containsSize; i++) 
                {
                    matches = (boolean) matcher.apply(i);
                }
                if (matches ^ isNegated()) {
                    break;
                }
            }
            if (line != null) {
                return read();
            }
        }
        return ch;
    }
}