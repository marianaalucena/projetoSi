package com.ufcg.si1.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.ufcg.si1.model.DTO.LoteDTO;
import com.ufcg.si1.model.Available;
import com.ufcg.si1.model.Lote;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.ufcg.si1.model.Produto;
import com.ufcg.si1.model.Unavailable;
import com.ufcg.si1.service.LoteService;
import com.ufcg.si1.service.LoteServiceImpl;
import com.ufcg.si1.service.ProdutoService;
import com.ufcg.si1.service.ProdutoServiceImpl;
import com.ufcg.si1.util.CustomErrorType;

import exceptions.ObjetoInvalidoException;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class RestApiController {

	ProdutoService produtoService = new ProdutoServiceImpl();
	LoteService loteService = new LoteServiceImpl();

	// -------------------Retrieve All
	// Products---------------------------------------------

	@RequestMapping(value = "/produto/", method = RequestMethod.GET)
	public ResponseEntity<List<Produto>> listAllUsers() {
		List<Produto> produtos = produtoService.findAllProdutos();

		if (produtos.isEmpty()) {
			return new ResponseEntity(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Produto>>(produtos, HttpStatus.OK);
	}

	// -------------------Criar um
	// Produto-------------------------------------------

	@RequestMapping(value = "/produto/", method = RequestMethod.POST)
	public ResponseEntity<?> criarProduto(@RequestBody Produto produto, UriComponentsBuilder ucBuilder) {

	
		if (produtoExiste(produto)) {
			return new ResponseEntity(new CustomErrorType("O produto " + produto.getNome() + " do fabricante "
					+ produto.getFabricante() + " ja esta cadastrado!"), HttpStatus.CONFLICT);
		}

		try {
			produto.mudaSituacao( new Unavailable()); //Produto.INDISPONIVEL
		} catch (ObjetoInvalidoException e) {
			return new ResponseEntity(new CustomErrorType("Error: Produto" + produto.getNome() + " do fabricante "
					+ produto.getFabricante() + " alguma coisa errada aconteceu!"), HttpStatus.NOT_ACCEPTABLE);
		}

		produtoService.saveProduto(produto);

		return new ResponseEntity<Produto>(produto, HttpStatus.CREATED);
	}
	
	private boolean produtoExiste(Produto produto) {
		boolean produtoExiste = false;

		for (Produto p : produtoService.findAllProdutos()) {
			if (p.getCodigoBarra().equals(produto.getCodigoBarra())) {
				produtoExiste = true;
			}
		} return produtoExiste;

	}

	@RequestMapping(value = "/produto/{busca}", method = RequestMethod.GET)
	public ResponseEntity<?> consultarProduto(@PathVariable("busca") String busca) {

		List<Produto> produtos = new ArrayList<>();

		for (Produto produto : produtoService.findAllProdutos()) {
			if (produto.getNome().contains(busca)) {
				produtos.add(produto);
			}
		}

		if (produtos.size() == 0) {
			return new ResponseEntity(new CustomErrorType("Produts with name " + busca + " not found"),
					HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<List<Produto>>(produtos, HttpStatus.OK);
	}

	@RequestMapping(value = "/produto/{id}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateProduto(@PathVariable("id") long id, @RequestBody Produto produto) {

		Produto currentProduto =  buscaPorId(id);

		if (currentProduto == null) {
			return new ResponseEntity(new CustomErrorType("Unable to upate. Produto with id " + id + " not found."),
					HttpStatus.NOT_FOUND);
		}


		currentProduto.updateProduto(produto);


		produtoService.updateProduto(currentProduto);
		return new ResponseEntity<Produto>(currentProduto, HttpStatus.OK);
	}

	@RequestMapping(value = "/produto/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteUser(@PathVariable("id") long id) {

		Produto user = buscaPorId(id);

		if (user == null) {
			return new ResponseEntity(new CustomErrorType("Unable to delete. Produto with id " + id + " not found."),
					HttpStatus.NOT_FOUND);
		}
		produtoService.deleteProdutoById(id);
		return new ResponseEntity<Produto>(HttpStatus.NO_CONTENT);
	}
	
	private Produto buscaPorId(long id) {
		Produto currentProduto = null;

		for (Produto p : produtoService.findAllProdutos()) {
			if (p.getId() == id) {
				currentProduto = p;
			}
		} return currentProduto;
	}

	@RequestMapping(value = "/produto/{id}/lote", method = RequestMethod.POST)
	public ResponseEntity<?> criarLote(@PathVariable("id") long produtoId, @RequestBody LoteDTO loteDTO) {
		Produto product = produtoService.findById(produtoId);

		if (product == null) {
			return new ResponseEntity(
					new CustomErrorType("Unable to create lote. Produto with id " + produtoId + " not found."),
					HttpStatus.NOT_FOUND);
		}

		Lote lote = loteService.saveLote(new Lote(product, loteDTO.getNumeroDeItens(), loteDTO.getDataDeValidade()));

		verificaCriacaoLote(product, loteDTO);

		return new ResponseEntity<>(lote, HttpStatus.CREATED);
	}

	private void verificaCriacaoLote(Produto product, LoteDTO loteDTO) {
		//try {
			if (product.getState() instanceof Unavailable) {   //product.getSituacao() == Produto.INDISPONIVEL
				if (loteDTO.getNumeroDeItens() > 0) {
					Produto produtoDisponivel = product;
					produtoDisponivel.state = new Available();  //produtoDisponivel.situacao = Produto.DISPONIVEL;
					produtoService.updateProduto(produtoDisponivel);
				}
			}
		//} catch (ObjetoInvalidoException e){
		//	e.printStackTrace();
		//}

	}

	@RequestMapping(value = "/lote/", method = RequestMethod.GET)
	public ResponseEntity<List<Lote>> listAllLotess() {
		List<Lote> lotes = loteService.findAllLotes();

		if (lotes.isEmpty()) {
			return new ResponseEntity(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Lote>>(lotes, HttpStatus.OK);
	}
}
