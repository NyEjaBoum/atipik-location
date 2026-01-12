package ws;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(ProformaWS.class);
        classes.add(ReservationWS.class);
        classes.add(CaisseWS.class);
        classes.add(VenteWS.class);
        classes.add(CheckinWS.class);
        classes.add(CheckoutWS.class);

        // Ajouter d'autres WS ici
        return classes;
    }
}