package be.businesstraining.repsoitory;

import java.util.List;

import be.businesstraining.domain.Product;

public interface IProductsRepository {

	List<Product> findAll();
	void addProduct(Product p);

}