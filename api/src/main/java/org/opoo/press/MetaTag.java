/*
 * Copyright 2013-2015 Alex Lin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opoo.press;

import java.util.List;

/**
 * @author Alex Lin
 */
public interface MetaTag {

    String getSlug();

    String getName();

    boolean isNameOrSlug(String nameOrSlug);

    List<Page> getPages();

    /**
     * The generated page of this tag.
     * @return the generated page of this tag.
     */
    Page getPage();

    Config getConfig();

    //<C extends Config> C getConfig(Class<C> clz);

    void setPage(Page page);
}
