package org.lulz.tiger.backend.liveness;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DisjointWeb {

    private Map<Web, Web> parent;
    private Map<Web, Integer> rank;

    public DisjointWeb() {
        parent = new HashMap<>();
        rank = new HashMap<>();
    }

    public void addWeb(Web web) {
        parent.put(web, web);
        rank.put(web, 0);
    }

    public void addWebs(Collection<Web> webs) {
        for (Web web: webs) {
            parent.put(web, web);
            rank.put(web, 0);
        }
    }

    public Web find(Web web)
    {
        if (parent.get(web) != web)
            parent.put(web, find(parent.get(web)));
        return parent.get(web);
    }

    public void union(Web a, Web b)
    {
        Web x = find(a);
        Web y = find(b);

        if (x == y)
            return;

        if (rank.get(x) > rank.get(y)) {
            parent.put(y, x);
            x.getRange().addAll(y.getRange());
            x.addSpillCost(y.getSpillCost());
        } else if (rank.get(x) < rank.get(y)) {
            parent.put(x, y);
            y.getRange().addAll(x.getRange());
            y.addSpillCost(x.getSpillCost());
        } else {
            parent.put(x, y);
            rank.put(y, rank.get(y) + 1);
            y.getRange().addAll(x.getRange());
            y.addSpillCost(x.getSpillCost());
        }
    }

}
