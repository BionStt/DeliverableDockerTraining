import { Component, OnInit } from '@angular/core';
import { ProductsService } from 'src/app/services/products.service';
import { IProduct } from 'src/app/domain/iproduct';

@Component({
  selector: 'app-products',
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss']
})
export class ProductsComponent implements OnInit {

  allProducts : IProduct[];
  constructor(private service: ProductsService) { }

  ngOnInit(): void {

   this.service.getProducts().subscribe(
     prod => this.allProducts = prod,
     err => console.log('== LOG : Some problem when calling the service. '+ err)
   );
  }

}
