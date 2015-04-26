package pkg.order;
import java.util.ArrayList;
import java.util.HashMap;
import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.*;
public class OrderBook {
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;
	PriceSetter priceSetter;
	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
		priceSetter = new PriceSetter();
	}

	public void addToOrderBook(Order order) {
		// Populate the buyOrders and sellOrders data structures, whichever
		// appropriate
		if (BuyOrder.class.isInstance(order)){
			if (this.buyOrders.containsKey(order.getStockSymbol())){
				ArrayList<Order> buyOrderList =buyOrders.get(order.getStockSymbol());
				buyOrderList.add(order);
			}
			else{
				ArrayList<Order> buyOrderList = new ArrayList<Order>();
				buyOrderList.add(order);
				this.buyOrders.put(order.getStockSymbol(), buyOrderList);
			}
		}
		else{
			if (this.sellOrders.containsKey(order.getStockSymbol())){
				ArrayList<Order> sellOrderList =sellOrders.get(order.getStockSymbol());
				sellOrderList.add(order);
			}
			else{
				ArrayList<Order> sellOrderList = new ArrayList<Order>();
				sellOrderList.add(order);
				this.sellOrders.put(order.getStockSymbol(), sellOrderList);
			}
		}
		
	}

	public void trade() {
		// Complete the trading.
		// 1. Follow and create the orderbook data representation (see spec)
		// 2. Find the matching price
		// 3. Update the stocks price in the market using the PriceSetter.
		// Note that PriceSetter follows the Observer pattern. Use the pattern.
		// 4. Remove the traded orders from the orderbook
		// 5. Delegate to trader that the trade has been made, so that the
		// trader's orders can be placed to his possession (a trader's position
		// is the stocks he owns)
		// (Add other methods as necessary)
		for (String stockSymbol : this.sellOrders.keySet()){
			if (this.buyOrders.containsKey(stockSymbol)){
				double matchPrice  = matchingPrice(this.sellOrders.get(stockSymbol), this.buyOrders.get(stockSymbol));
				if (matchPrice>0){
					try{
						m.updateStockPrice(stockSymbol, matchPrice);
					}
					catch (StockMarketExpection e){};
//					IObserver observer = new IObserver();
//					priceSetter.registerObserver(observer);
//					priceSetter.setNewPrice(m, stockSymbol, matchPrice);
					int stockTradeSize =0;
					
				    for (Order sellOrder : this.sellOrders.get(stockSymbol)){
				    	if (sellOrder.getPrice()<=matchPrice){
				    		try{
				    			sellOrder.getTrader().tradePerformed(sellOrder, matchPrice);
				    			stockTradeSize += sellOrder.getSize();
				    		}
				    		catch (StockMarketExpection e){};
				    		this.sellOrders.remove(sellOrder);
				    	}
				    }
				    ArrayList<Order> tempOrders  = sortOrderPrice(this.buyOrders.get(stockSymbol));
				    this.buyOrders.remove(stockSymbol);
				    this.buyOrders.put(stockSymbol,tempOrders);
				    for (Order buyOrder : this.buyOrders.get(stockSymbol)){
				    	if (buyOrder.getPrice()>=matchPrice && stockTradeSize>0){
				    		try{
				    			if (stockTradeSize>buyOrder.getSize())
				    			{
				    				buyOrder.getTrader().tradePerformed(buyOrder, matchPrice);
				    				stockTradeSize -= buyOrder.getSize();
				    			}
				    			else{
				    				buyOrder.setSize(stockTradeSize);
				    				buyOrder.getTrader().tradePerformed(buyOrder, matchPrice);
				    				stockTradeSize = 0;
				    			}
				    							
				    		}
				    		catch (StockMarketExpection e){};
				    		this.buyOrders.remove(buyOrder);
				    	}
				    }
				}
			}
		}
	}
	private double matchingPrice(ArrayList<Order> sellOrderList, ArrayList<Order> buyOrderList){
		ArrayList<double[]> sellPriceList = new  ArrayList<double[]>();
		ArrayList<double[]> buyPriceList = new ArrayList<double[]>();
		for (int i = 0; i<sellOrderList.size(); i++){
			double[] priceSet = new double[3];
			priceSet[0] = sellOrderList.get(i).getPrice();
			priceSet[1] = sellOrderList.get(i).getSize();
			boolean flag = true;
			for (int j =0; j<sellPriceList.size();j++){
				if (priceSet[0]<sellPriceList.get(j)[0]){
					sellPriceList.add(j,priceSet);
					flag = false;
					break;
				}
			}
			if (flag){
				sellPriceList.add(priceSet);
			}
		}
		for (int i = 0; i<buyOrderList.size(); i++){
			double priceSet[] = new double[3];
			priceSet[0] = buyOrderList.get(i).getPrice();
			priceSet[1] = buyOrderList.get(i).getSize();
			boolean flag = true;
			for (int j =0; j<buyPriceList.size(); j++){
				if (priceSet[0]<buyPriceList.get(j)[0]){
					buyPriceList.add(j,priceSet);
					flag = false;
					break;
				}
			}
			if (flag){
				buyPriceList.add(priceSet);
			}
		}
		////////////////////get the arraylist for price matching
		
		for(int i = 0; i<sellPriceList.size(); i++){
			if(i>0){
				sellPriceList.get(i)[2] =sellPriceList.get(i-1)[2] + sellPriceList.get(i)[1];
			}
			else{
				sellPriceList.get(0)[2] = sellPriceList.get(0)[1];
			}
			
		}
		
		for(int i = buyPriceList.size()-1; i>=0; i-- ){
			if (i<buyPriceList.size()-1){
				buyPriceList.get(i)[2] = buyPriceList.get(i+1)[2] +buyPriceList.get(i)[1];
			}
			else{
				buyPriceList.get(i)[2] = buyPriceList.get(i)[1];
			}
			
		}
		//get the price
		double matchPrice =0;
		for (int i = sellPriceList.size()-1; i>=0; i--){
			double bidPrice = sellPriceList.get(i)[0];
			double sellAmount = sellPriceList.get(i)[2];
			double buyAmount = 0;
			for (int j = 0;j <buyPriceList.size();j++ ){
				if (buyPriceList.get(j)[0]>=bidPrice){
					buyAmount = buyPriceList.get(j)[2];
					break;
				}
			}
			if (sellAmount<=buyAmount){
				matchPrice = bidPrice;
				break;
			}
		}
		return matchPrice; 
	}
	private ArrayList<Order> sortOrderPrice(ArrayList<Order> orders){
		ArrayList<Order> sortedOrders = new ArrayList<Order>();
		boolean flag = true;
		sortedOrders.add(orders.get(0));
		for (int i =1; i<orders.size(); i++){
			for (int j=0; j<sortedOrders.size();j++){
				if (orders.get(i).getPrice()>sortedOrders.get(j).getPrice()){
					sortedOrders.add(j,orders.get(i));
					flag = false;
					break;
				}
			}
			if (flag){
				sortedOrders.add(orders.get(i));
			}
		}
		return sortedOrders;
	}
	
}
