package dev.jdguo.market.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarketLayoutTest {
 @Test void scaledViewportsStayInsideBounds() {
  int[][] sizes={{320,240},{427,240},{480,270},{640,360},{660,360},{854,480},{960,540},{1280,720},{1920,1080}};
  for(int[] size:sizes){var l=MarketLayout.at(size[0],size[1]);
   for(var b:new MarketLayout.Box[]{l.listings(),l.details(),l.order()}){
    assertTrue(b.x()>=0&&b.y()>=62); assertTrue(b.width()>0&&b.height()>=112);
    assertTrue(b.x()+b.width()<=size[0]);assertTrue(b.y()+b.height()<=size[1]-48);
    // Currency selector, two labelled fields and submit button all stay in the order pane.
    assertTrue(b.y()+25+68+18<=b.y()+b.height());
   }
   if(!l.compact()) {assertTrue(l.listings().x()+l.listings().width()<l.details().x());assertTrue(l.details().x()+l.details().width()<l.order().x());}
  }
 }
 @Test void narrowUsesTabsInsteadOfSqueezedColumns(){assertTrue(MarketLayout.at(640,360).compact());assertFalse(MarketLayout.at(660,360).compact());}
}
