import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tries {
    private TrieNode root = new TrieNode();

    private class TrieNode {
        private Boolean isKey = false;
        private Map<Character, TrieNode> links = new HashMap<>();
        private String name;
    }

    public void add(String s) {
        TrieNode curr = root;
        for (int i = 0; i < s.length(); i++) {
            Character c = Character.toLowerCase(s.charAt(i));
            if (curr.links.containsKey(c)) {
                curr = curr.links.get(c);
            } else {
                TrieNode next = new TrieNode();
                curr.links.put(c, next);
                curr = next;
            }
        }
        curr.isKey = true;
        curr.name = s;
    }

    public List<String> withPrefix(String prefix) {
        List<String> results = new LinkedList<>();
        TrieNode next = getNext(root, prefix, 0);
        collect(next, new StringBuilder(prefix), results);
        return results;
    }

    private TrieNode getNext(TrieNode n, String s, int d) {
        if (n == null) {
            return null;
        }
        if (d == s.length()) {
            return n;
        }
        Character c = s.charAt(d);
        return getNext(n.links.get(c), s, d + 1);
    }

    private void collect(TrieNode next, StringBuilder prefix, List<String> results) {
        if (next == null) {
            return;
        }
        if (next.isKey) {
            results.add(next.name);
        }
        for (Character c : next.links.keySet()) {
            prefix.append(c);
            collect(next.links.get(c), prefix, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }
}
