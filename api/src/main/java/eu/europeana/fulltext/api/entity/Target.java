/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.api.entity;

import org.mongodb.morphia.annotations.Embedded;

/**
 * Created by luthien on 26/06/2018.
 */
@Embedded
public class Target {

    private Integer X;
    private Integer Y;
    private Integer W;
    private Integer H;

    public Target(Integer X, Integer Y, Integer W, Integer H) {
        this.X = X;
        this.Y = Y;
        this.W = W;
        this.H = H;
    }

    public Integer getX() {
        return X;
    }

    public void setX(Integer x) {
        this.X = x;
    }

    public Integer getY() {
        return Y;
    }

    public void setY(Integer y) {
        this.Y = y;
    }

    public Integer getW() {
        return W;
    }

    public void setW(Integer w) {
        this.W = w;
    }

    public Integer getH() {
        return H;
    }

    public void setH(Integer h) {
        this.H = h;
    }

}
