/**
 * Iñigo Alonso - 665959
 * Alejandro Dieste - 541892
 */
package trazadorrayos;

import java.awt.Color;
import java.util.ArrayList;
import javax.vecmath.Point4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

public class OperacionesEscena {

	/**
	 * Devuelve la figura intersectada más cercana 
	 * @param figuras
	 * @param punto
	 * @param rayo
	 * @param camara
	 * @param desde
	 * @return
	 */
	public static Figura FiguraMasCercana(ArrayList<Figura> figuras,
			Point4d punto, Rayo rayo, Camara camara, Figura desde) {
		Figura figura = null;
		double distanciaMenor = Double.MAX_VALUE;
		for (int i = 0; i < figuras.size(); i++) {
			Figura siguienteFigura = figuras.get(i);
			if (desde != siguienteFigura) {
				Point4d puntoInterseccion = Interseccion.intersecta(rayo,
						siguienteFigura);
				if (puntoInterseccion != null && puntoInterseccion.x != punto.x
						&& puntoInterseccion.y != punto.y
						&& puntoInterseccion.z != punto.z) {
					double distancia = punto.distanceSquared(puntoInterseccion);
					if (distancia < distanciaMenor) {
						distanciaMenor = distancia;
						figura = siguienteFigura;
					}
				}
			}
		}
		return figura;
	}

	public static Color colorPuntoPantalla(Point4d puntoPixelPantalla,
			Escena escena, int maxDepth) {
		Color color = new Color(0, 0, 0);
		Vector3d sumaColor = new Vector3d(0, 0, 0);
		/*
		 * Ahora se crea un rayo que pase por el punto de la pantalla y con
		 * direccion (puntoCamara-puntopantalla)
		 */
		Vector4d direccion = new Vector4d();
		direccion.x = (puntoPixelPantalla.x - escena.getCamara().getPosicion().x);
		direccion.y = (puntoPixelPantalla.y - escena.getCamara().getPosicion().y);
		direccion.z = (puntoPixelPantalla.z - escena.getCamara().getPosicion().z);
		direccion.w = 0;
		for (int i = 0; i < escena.getFocos().size(); i++) {
			Rayo rayo = new Rayo(direccion, escena.getCamara().getPosicion(),
					escena.getFocos().get(i).getColor());
			/*
			 * Ahora hay que ver si el rayo intersecta con alguno de los objetos
			 * de la escena, y si lo hace, quedarnos unicamente con el objeto mas
			 * cercano a la pantalla
			 */
			color = colorDesdeRayo(escena, rayo, maxDepth, null, false, i);
			if (sumaColor.x < color.getRed()) {
				sumaColor.x = color.getRed();
			}
			if (sumaColor.y < color.getGreen()) {
				sumaColor.y = color.getGreen();
			}
			if (sumaColor.z < color.getBlue()) {
				sumaColor.z = color.getBlue();
			}
		}
		color = new Color((int) sumaColor.x, (int) sumaColor.y,
				(int) sumaColor.z);
		return color;
	}

	/**
	 * Devuelve el color que devuelve un rayo al golpear una figura en un punto
	 */
	public static Color colorDesdeRayo(Escena escena, Rayo rayo, int MaxDepth,
			Figura desde, boolean refractadoParaSalir, int numFoco) {
		Color color = null;
		Figura figura = null;
		if (refractadoParaSalir) {
			figura = OperacionesEscena.FiguraMasCercana(escena.getFiguras(),
					rayo.getPunto(), rayo, escena.getCamara(), null);
		}
		else {
			figura = OperacionesEscena.FiguraMasCercana(escena.getFiguras(),
					rayo.getPunto(), rayo, escena.getCamara(), desde);
		}

		if (figura != null && Interseccion.intersecta(rayo, figura) != null) {
			/*
			 * Ahora tenemos que intersecta con la figura y tenemos que obtener
			 * su color
			 */
			Point4d punto = Interseccion.intersecta(rayo, figura);
			double refrac = 1;
			if (figura.getIndiceRefraccion() != 0)
				refrac = 1 / figura.getIndiceRefraccion();// suponemos que el
															// exterior es 1
			/*
			 * Ahora se calcula la direccion del angulo refractado y del angulo
			 * reflejado
			 */
			Vector4d normal = figura.getNormal(punto);
			normal.normalize();
			rayo.getDireccion().normalize();
			Vector4d reflejado = rayoReflejado(normal, rayo);
			Vector4d direccionRayo = new Vector4d(rayo.getDireccion());
			direccionRayo.negate();
			Vector4d refractado = rayoRefractado(refrac, normal, rayo);
			/*
			 * Ahora tienes el angulo refractado y el angulo reflejado, ahora
			 * para calcular el color, primero tienes que mirar el color que
			 * tiene tu figura y la intensidad del rayo que ha impacado contigo,
			 * y luego sumarle el color que te devuelvan los dos rayos que salen
			 * de ese punto (reflejado y refractado) y normalizar
			 *
			 *
			 * primero tienes que mirar si desde ese punto hasta el foco no hay
			 * inersecciones, si las hay, devuelves el llamado primer modelo de
			 * iliminacion, es decir solo la luz ambiental, sino hay
			 * intereseccion ya pasas al tercer modelo
			 */
			double noVisible = interseccionAFocoTapado(punto, escena.getFocos()
					.get(numFoco), escena.getFiguras(), figura);
			if (refractadoParaSalir) {
				double intensidadRefractadoR = escena.getFocos().get(numFoco)
						.getColor().getRed();
				double intensidadRefractadoG = escena.getFocos().get(numFoco)
						.getColor().getGreen();
				double intensidadRefractadoB = escena.getFocos().get(numFoco)
						.getColor().getBlue();
				Color colorRayoRefractado = new Color(
						(int) intensidadRefractadoR,
						(int) intensidadRefractadoG,
						(int) intensidadRefractadoB);
				Rayo rayoRefractado = new Rayo(refractado, punto,
						colorRayoRefractado);
				return colorDesdeRayo(escena, rayoRefractado, MaxDepth--,
						figura, false, numFoco);
			}
			else {
				if (noVisible == 1) {
					double intensidad = escena.getCamara()
							.getIntensidadAmbiente();
					// color=I*Kd
					int red = (int) (intensidad * figura.kd.getRed());
					int blue = (int) (intensidad * figura.kd.getBlue());
					int green = (int) (intensidad * figura.kd.getGreen());
					color = new Color(red, green, blue);
				}
				else {
					if (figura.getAmbiental()) {
						// figura solo ambiental
						double intensidad = escena.getCamara()
								.getIntensidadAmbiente();
						// color=I*Kd
						int red = (int) (intensidad * figura.kd.getRed());
						int blue = (int) (intensidad * figura.kd.getBlue());
						int green = (int) (intensidad * figura.kd.getGreen());
						color = new Color(red, green, blue);
					}
					else {
						Intensidad intensidadPunto = new Intensidad();
						/**
						 * calculamos la intensidad en el punto, en RGB la
						 * ambiental, la difusa y la especular
						 */
						intensidadPunto.calcularIntensidadPunto(noVisible,
								punto, escena, figura, rayo, numFoco);
						Rayo rayoReflejado = new Rayo(reflejado, punto, null);
						Rayo rayoRefractado = new Rayo(refractado, punto, null);
						// si aun no se ha llegado al limite y la figura es refractante
						if (MaxDepth > 0 && figura.getIndiceReflectividad() > 0) {
							Color colorReflejado = colorDesdeRayo(escena,
									rayoReflejado, MaxDepth--, figura, false,
									numFoco);
							intensidadPunto.red += figura
									.getIndiceReflectividad()
									* colorReflejado.getRed();
							intensidadPunto.blue += figura
									.getIndiceReflectividad()
									* colorReflejado.getBlue();
							intensidadPunto.green += figura
									.getIndiceReflectividad()
									* colorReflejado.getGreen();
						}
						// si hay suficiente intensidad refractada, lanzamos el
						// rayo
						if (MaxDepth > 0 && figura.getTransparencia() > 0) {
							boolean esperarRefraccion = false;
							if (figura instanceof Esfera) {
								esperarRefraccion = true;
							}
							Color colorRefractado = colorDesdeRayo(escena,
									rayoRefractado, MaxDepth--, figura,
									esperarRefraccion, numFoco);
							// intensidad+(indiceTransparencia*intensidadRefrac)
							intensidadPunto.red += figura.getTransparencia()
									* colorRefractado.getRed();
							intensidadPunto.blue += figura.getTransparencia()
									* colorRefractado.getBlue();
							intensidadPunto.green += figura.getTransparencia()
									* colorRefractado.getGreen();
						}
						/*
						 * Si el punto en e que has calculado no es del todo
						 * visible debido a que hay objetos translucidos no
						 * totales, hay que recalcular la intensidad
						 */
						double indiceVisibilidad = 1 - noVisible;
						intensidadPunto.red = (int) (intensidadPunto.red * indiceVisibilidad);
						intensidadPunto.blue = (int) (intensidadPunto.blue * indiceVisibilidad);
						intensidadPunto.green = (int) (intensidadPunto.green * indiceVisibilidad);
						color = normalizarColor(intensidadPunto.red,
								intensidadPunto.green, intensidadPunto.blue);
					}
				}
			}
		}
		else {
			// No se intersecta con nada
			color = new Color(0, 0, 0);
		}
		return color;
	}

	/**
	 * Si la intensidad de algun color sobre pasa el limite, se capa a 255
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	public static Color normalizarColor(int red, int green, int blue) {
		int mayor = 0;
		if (blue > mayor) mayor = blue;
		if (red > mayor) mayor = red;
		if (green > mayor) mayor = green;
		if (red > 255) red = 255;
		if (green > 255) green = 255;
		if (blue > 255) blue = 255;
		return new Color(red, green, blue);
	}

	/**
	 * Devuelve el rayo reflejado del rayo incidente
	 * 
	 * @param normal
	 * @param rayo
	 * @return
	 */
	public static Vector4d rayoReflejado(Vector4d normal, Rayo rayo) {
		Vector4d reflejado = new Vector4d(normal);
		// 2*(V*N)N
		double aux = 2 * normal.dot(rayo.getDireccion());
		reflejado.scale(aux);
		reflejado.w = normal.w;
		// V-2*(V*N)N
		reflejado.sub(rayo.getDireccion(), reflejado);
		reflejado.normalize();
		return reflejado;
	}

	/**
	 * Devuelve el rayo refractado del rayo incidente
	 * 
	 * @param refrac
	 * @param normal
	 * @param rayo
	 * @return
	 */
	public static Vector4d rayoRefractado(double refrac, Vector4d normal,
			Rayo rayo) {
		Vector4d refractado = new Vector4d(normal);
		Vector4d direccionRayo = new Vector4d(rayo.getDireccion());
		direccionRayo.negate();
		// auxR=(n(N.I)-sqrt(1-n^2(1-(N.I)^2)))
		double auxR = (refrac * (normal.dot(direccionRayo)))
				- Math.sqrt(1 - (refrac * refrac * (1 - (normal
						.dot(direccionRayo) * normal.dot(direccionRayo)))));
		// refractado=auxR*N-n*I
		refractado.scale(auxR);
		direccionRayo.scale(refrac);
		refractado.sub(refractado, direccionRayo);
		refractado.w = 0;
		refractado.normalize();
		return refractado;
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
	public static double interseccionAFocoTapado(Point4d punto, Foco foco,
			ArrayList<Figura> figuras, Figura figura) {
		double intersecta = 0;
		Vector4d rayoAlFoco = new Vector4d();
		rayoAlFoco.sub(foco.getPosicion(), punto);
		rayoAlFoco.w = 0;
		rayoAlFoco.normalize();
		// cos(L, N)
		double cosenoDif = rayoAlFoco.dot(figura.getNormal(punto))
				/ figura.getNormal(punto).length() / rayoAlFoco.length();
		if (cosenoDif < 0) {
			// solo se ve si la figura refracta
			intersecta = (1 - figura.getTransparencia());
		}
		// miramos si intersectamos con alguna figura de la escena
		for (int i = 0; i < figuras.size() && intersecta != 1; i++) {
			Rayo rayoPuntoFoco = new Rayo(rayoAlFoco, punto);
			Point4d puntoInterseccion = Interseccion.intersecta(rayoPuntoFoco,
					figuras.get(i));
			// si hay interseccion, hay que mirar si esta mas cerca del foco, o
			// si es visible esa interseccion
			if (puntoInterseccion != null) {
				double distanciaPuntoFoco = punto.distanceSquared(foco
						.getPosicion());
				double distanciaInterseccionPunto = puntoInterseccion
						.distanceSquared(punto);
				if (distanciaInterseccionPunto < distanciaPuntoFoco
						&& intersecta < (1 - figuras.get(i).getTransparencia())) {
					intersecta = (1 - figuras.get(i).getTransparencia());
				}
			}
		}
		return intersecta;
	}
}