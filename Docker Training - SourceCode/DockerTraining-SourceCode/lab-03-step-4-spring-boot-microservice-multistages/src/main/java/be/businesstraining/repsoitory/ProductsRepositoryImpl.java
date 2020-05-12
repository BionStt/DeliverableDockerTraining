package be.businesstraining.repsoitory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import be.businesstraining.domain.Product;

@Repository
public class ProductsRepositoryImpl implements IProductsRepository {

	@SuppressWarnings("serial")
	private static List<Product> fakeDB = new ArrayList<Product>(){{
			add(new Product("P100", "Product One", BigDecimal.valueOf(100.5)));
			add(new Product("P200", "Product Two", BigDecimal.valueOf(200.5)));
			add(new Product("P300", "Product Three", BigDecimal.valueOf(300.5)));
	}};

	@Override
	public List<Product> findAll() {
         return fakeDB;
	}
	@Override
	public void addProduct(Product p) {
		fakeDB.add(p);
	}

}
