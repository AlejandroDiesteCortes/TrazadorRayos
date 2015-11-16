/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trazadorrayos;

import java.awt.Color;
import java.util.ArrayList;

import javax.vecmath.Point4d;
import javax.vecmath.Vector4d;

/**
 *
 * @author shathe
 */
public class OperacionesEscena {

	public static Figura FiguraMasCercana(ArrayList<Figura> figuras,
			Point4d punto, Rayo rayo, Camara camara) {
		Figura figura = null;
		double distanciaMenor = Double.MAX_VALUE;
		for (int i = 0; i < figuras.size(); i++) {
			Figura siguienteFigura = figuras.get(i);
			Point4d puntoInterseccion = Interseccion.intersecta(rayo,
					siguienteFigura, camara);
			if (puntoInterseccion != null) {
				double distancia = punto.distanceSquared(puntoInterseccion);
				if (distancia < distanciaMenor) {
					distanciaMenor = distancia;
					figura = siguienteFigura;
				}

			}
		}

		return figura;
	}

	public static Color colorPuntoPantalla(Point4d puntoPixelPantalla,
			Escena escena, int maxDepth, double minIntensity) {
		Color color = new Color(0, 0, 0);

		/*
		 * Ahora se crea un rayo que pase por el punto de la pantalla y con
		 * direccion (puntoCamara-puntopantalla)
		 */
		Vector4d direccion = new Vector4d();
		direccion.x = (puntoPixelPantalla.x - escena.getCamara().getPosicion().x);
		direccion.y = (puntoPixelPantalla.y - escena.getCamara().getPosicion().y);
		direccion.z = (puntoPixelPantalla.z - escena.getCamara().getPosicion().z);
		direccion.w = 0;
		Rayo rayo = new Rayo(direccion, puntoPixelPantalla, new Color(255, 255,
				255), 1);
		/*
		 * Ahora hay que ver si el rayo intersecta con alguno de los objetos de
		 * la escen, y si lo hace, quedarnos unicamente con el objeto mas
		 * cercano a pa pantalla
		 */
		color = colorDesdeRayo(escena, rayo, maxDepth, minIntensity);
		// ¿aqui normalizar eel color que de devuelve con el de la luz del foco?
		return color;

	}

	/*
	 * Devuelve el color que devuelve un rayo al golpear una figura en un punto
	 */
	public static Color colorDesdeRayo(Escena escena, Rayo rayo, int MaxDepth,
			double minIntensity) {
		Color color = new Color(0, 0, 0);

		Figura figura = OperacionesEscena.FiguraMasCercana(escena.getFiguras(),
				rayo.getPunto(), rayo, escena.getCamara());
		if (figura != null
				&& Interseccion.intersecta(rayo, figura, escena.getCamara()) != null) {
			/*
			 * Ahora tenemos que intersecta con la figura y tenemos que obtener
			 * su color
			 */
			Point4d punto = Interseccion.intersecta(rayo, figura,
					escena.getCamara());

			double reflec = figura.getIndiceReflectividad();
			double refrac = figura.getIndiceRefraccion();
			double kd = figura.getIndiceEspecularKS();
			double ks = figura.getIndiceEspecularKS();

			/*
			 * Ahora se calcula la direccion del angulo refractado y del angulo
			 * reflejado
			 */

			Vector4d refractado = new Vector4d();

			Vector4d normal = figura.getNormal(punto);

			// reflejado => V-2*(V*N)N

			// aux=2*(V*N)
			double aux = 2 * normal.dot(rayo.getDireccion());
			Vector4d reflejado = new Vector4d();
			// 2*(V*N)N
			reflejado.x = aux * normal.x;
			reflejado.y = aux * normal.y;
			reflejado.z = aux * normal.z;
			reflejado.w = normal.w;
			// V-2*(V*N)N
			reflejado.x = normal.x - reflejado.x;
			reflejado.z = normal.z - reflejado.z;
			reflejado.y = normal.y - reflejado.y;
			reflejado.w = normal.w - reflejado.w;

			// ahora calculas el angulo reflejado
			// T=(Iref*(normal*Rayo)-Raiz(1-Iref²(1-(normal*rayo)²)))
			// *normal-Iref*rayo
			double auxR = refrac
					* (normal.dot(rayo.getDireccion()))
					- Math.sqrt(1
							- refrac
							* refrac
							* (1 - normal.dot(rayo.getDireccion())
									* normal.dot(rayo.getDireccion())));

			refractado.x = auxR * normal.x - refrac * rayo.getDireccion().x;
			refractado.y = auxR * normal.y - refrac * rayo.getDireccion().y;
			refractado.z = auxR * normal.z - refrac * rayo.getDireccion().z;
			refractado.w = auxR * normal.w - refrac * rayo.getDireccion().w;

			/*
			 * Ahora tienes el angulo refractado y el angulo reflejado, ahora
			 * para calcular el color, primero tienes que mirar el color que
			 * tiene tu figura y la intensidad del rayo que ha impacado contigo,
			 * y luego sumarle el color que te devuelvan los dos rayos que salen
			 * de ese punto (reflejado y refractado) y normalizar
			 */

			/*
			 * primero tienes que mirar si desde ese punto hasta el foco no hay
			 * inersecciones, si las hay, devuelves el llamado primer modelo de
			 * iliminacion, es decir solo la luz ambiental, sino hay
			 * intereseccion ya pasas al tercer modelo
			 */

			boolean noVisible = interseccionAFocoTapado(punto,
					escena.getFoco(), escena.getFiguras(), figura);

			if (noVisible) {
				double intensidad = escena.getFoco().getIntensidadAmbiente()
						* kd;
				int red = (int) intensidad * figura.color.getRed();
				int blue = (int) intensidad * figura.color.getBlue();
				int green = (int) intensidad * figura.color.getGreen();
				color = new Color(red, green, blue);

			}
			else {
				// Calculas la intensidad en tu punto
				Vector4d rayoAlOjo = new Vector4d();
				rayoAlOjo.x = escena.getCamara().getPosicion().x - punto.x;
				rayoAlOjo.y = escena.getCamara().getPosicion().y - punto.y;
				rayoAlOjo.z = escena.getCamara().getPosicion().z - punto.z;
				rayoAlOjo.w = 0;
				Vector4d rayoAlFoco = new Vector4d();
				rayoAlFoco.x = escena.getFoco().getPosicion().x - punto.x;
				rayoAlFoco.y = escena.getFoco().getPosicion().y - punto.y;
				rayoAlFoco.z = escena.getFoco().getPosicion().z - punto.z;
				rayoAlFoco.w = 0;

				double intensidad = escena.getFoco().getIntensidadAmbiente()
						* kd + kd * rayo.getIntensidad()
						* rayoAlFoco.dot(normal) / normal.lengthSquared()
						/ rayoAlFoco.lengthSquared() + ks
						* rayo.getIntensidad() * reflejado.dot(rayoAlOjo)
						/ rayoAlOjo.lengthSquared() / reflejado.lengthSquared();

				int red = (int) (intensidad * figura.color.getRed());
				int blue = (int) (intensidad * figura.color.getBlue());
				int green = (int) (intensidad * figura.color.getGreen());

				Rayo rayoReflejado = new Rayo(reflejado, punto, color,
						rayo.getIntensidad() * reflec);
				Rayo rayoRefractado = new Rayo(refractado, punto, color,
						rayo.getIntensidad() * (1 - reflec));

				if (MaxDepth > 0
						&& rayoReflejado.getIntensidad() > minIntensity) {
					Color colorReflejado = colorDesdeRayo(escena,
							rayoReflejado, MaxDepth--, minIntensity);
					red += colorReflejado.getRed();
					blue += colorReflejado.getBlue();
					green += colorReflejado.getGreen();
				}
				if (MaxDepth > 0
						&& rayoRefractado.getIntensidad() > minIntensity) {
					Color colorRefractado = colorDesdeRayo(escena,
							rayoRefractado, MaxDepth--, minIntensity);
					red += colorRefractado.getRed();
					blue += colorRefractado.getBlue();
					green += colorRefractado.getGreen();

				}
				color = normalizarColor(red, green, blue);

			}

		}
		else {
			// No se intersecta con nada
			color = new Color(0, 0, 0);
		}
		return color;
	}

	public static Color normalizarColor(int red, int green, int blue) {
		int mayor = 0;
		if (blue > mayor) mayor = blue;
		if (red > mayor) mayor = red;
		if (green > mayor) mayor = green;
		if (mayor <= 255)
			return new Color(red, green, blue);
		else {
			double indiceReduccion = mayor / 255;
			double redreducido = red / indiceReduccion;
			double greenreducido = green / indiceReduccion;
			double bluereducido = blue / indiceReduccion;
			return new Color((int) redreducido, (int) greenreducido,
					(int) bluereducido);
		}

	}

	/**
	 * Funcion que devuelve true si el rayo que sale del foco intersecta con
	 * algo antes de llegar al punto, si no devuelve false
	 * 
	 * @param punto
	 * @param foco
	 * @param figuras
	 * @return
	 */
	public static boolean interseccionAFocoTapado(Point4d punto, Foco foco,
			ArrayList<Figura> figuras, Figura figura) {
		boolean intersecta = false;
		Camara cam = new Camara(foco.getPosicion());
		for (int i = 0; i < figuras.size() && !intersecta; i++) {

			if (figura != figuras.get(i)) {
				Vector4d direccionRayo = Interseccion.puntoMenosPunto(punto,
						foco.getPosicion());
				Rayo rayoPuntoFoco = new Rayo(direccionRayo, punto);
				if (Interseccion.intersecta(rayoPuntoFoco, figuras.get(i), cam) != null) {
					intersecta = true;
				}
			}
		}
		return intersecta;
	}
}