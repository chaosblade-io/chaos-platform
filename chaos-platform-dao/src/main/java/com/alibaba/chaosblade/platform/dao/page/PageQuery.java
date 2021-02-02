/*
 * Copyright 1999-2021 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.platform.dao.page;

import lombok.Data;

/**
 * @author yefei
 */
@Data
public abstract class PageQuery {

    public final static int PAGE = 1;

    public final static int PAGE_SIZE = 10;

    protected int page = PAGE;

    protected int pageSize = PAGE_SIZE;

    protected boolean paged = true;

    public void notPage() {
        paged = false;
    }

}
